package com.example.carromaim;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class AimOverlayService extends Service {
    WindowManager wm;
    AimView view;
    LinearLayout panel;
    float angle = -90f, power = .65f;
    boolean cushions = true, grid = false;

    int dp(float x) { return (int)(x * getResources().getDisplayMetrics().density + .5f); }

    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        build();
    }

    WindowManager.LayoutParams params(int w,int h) {
        int type = Build.VERSION.SDK_INT >= 26 ?
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
            WindowManager.LayoutParams.TYPE_PHONE;
        return new WindowManager.LayoutParams(w,h,type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT);
    }

    void build() {
        view = new AimView(this);
        wm.addView(view, params(-1,-1));

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16),dp(12),dp(16),dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(235,17,22,29));
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1),Color.argb(90,150,170,190));
        panel.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("CARROM AIM  •  PRACTICE");
        title.setTextColor(Color.WHITE);
        title.setTextSize(14);
        title.setTypeface(null,1);
        panel.addView(title);

        TextView hint = new TextView(this);
        hint.setText("Drag the striker marker on the board. Adjust angle and power.");
        hint.setTextColor(Color.LTGRAY);
        hint.setTextSize(11);
        panel.addView(hint);

        SeekBar angleBar = new SeekBar(this);
        angleBar.setMax(360);
        angleBar.setProgress((int)angle + 180);
        angleBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b,int p,boolean f){ angle=p-180; view.invalidate(); }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        panel.addView(angleBar);

        SeekBar powerBar = new SeekBar(this);
        powerBar.setMax(100);
        powerBar.setProgress((int)(power*100));
        powerBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b,int p,boolean f){ power=p/100f; view.invalidate(); }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        panel.addView(powerBar);

        LinearLayout row = new LinearLayout(this);
        Button bank = button("Bank: ON");
        bank.setOnClickListener(v->{ cushions=!cushions; bank.setText("Bank: "+(cushions?"ON":"OFF")); view.invalidate(); });
        row.addView(bank,new LinearLayout.LayoutParams(0,dp(42),1));

        Button gridBtn = button("Grid: OFF");
        gridBtn.setOnClickListener(v->{ grid=!grid; gridBtn.setText("Grid: "+(grid?"ON":"OFF")); view.invalidate(); });
        row.addView(gridBtn,new LinearLayout.LayoutParams(0,dp(42),1));

        Button reset = button("Reset");
        reset.setOnClickListener(v->{angle=-90;power=.65f;angleBar.setProgress(90);powerBar.setProgress(65);view.reset();});
        row.addView(reset,new LinearLayout.LayoutParams(0,dp(42),1));
        panel.addView(row);

        Button close=button("Close overlay");
        close.setOnClickListener(v->stopSelf());
        panel.addView(close);

        WindowManager.LayoutParams pp=params(dp(290),-2);
        pp.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL; pp.y=dp(16);
        pp.flags=WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        wm.addView(panel,pp);
    }

    Button button(String s){
        Button b=new Button(this); b.setText(s); b.setTextSize(11); b.setTextColor(Color.WHITE);
        return b;
    }

    @Override public void onDestroy(){
        if(view!=null) wm.removeViewImmediate(view);
        if(panel!=null) wm.removeViewImmediate(panel);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i){return null;}

    class AimView extends View {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG);
        float strikerX=-1,strikerY=-1;
        RectF board=new RectF();
        AimView(Context c){super(c); setLayerType(View.LAYER_TYPE_SOFTWARE,null);}

        void reset(){strikerX=-1;strikerY=-1;invalidate();}

        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight();
            float size=Math.min(w*.86f,h*.70f);
            float l=(w-size)/2,t=(h-size)/2+dp(35);
            board.set(l,t,l+size,t+size);

            p.setStyle(Paint.Style.FILL); p.setColor(Color.argb(45,255,255,255));
            c.drawRoundRect(board,dp(12),dp(12),p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));
            p.setColor(Color.argb(110,255,255,255));
            c.drawRoundRect(board,dp(12),dp(12),p);

            if(grid){
                p.setStrokeWidth(1);p.setColor(Color.argb(35,255,255,255));
                for(int i=1;i<10;i++){
                    float x=board.left+i*size/10f,y=board.top+i*size/10f;
                    c.drawLine(x,board.top,x,board.bottom,p);
                    c.drawLine(board.left,y,board.right,y,p);
                }
            }

            float sx=strikerX<0?board.centerX():strikerX;
            float sy=strikerY<0?board.bottom-size*.10f:strikerY;

            double r=Math.toRadians(angle);
            float dx=(float)Math.cos(r),dy=(float)Math.sin(r);
            float x=sx,y=sy,remaining=size*(.25f+.9f*power);
            Path path=new Path();path.moveTo(x,y);
            int hits=0;

            for(int n=0;n<5 && remaining>1;n++){
                float tx=x+dx*remaining,ty=y+dy*remaining;
                float hx=tx,hy=ty;int wall=0;
                if(tx<board.left){hx=board.left;hy=y+(board.left-x)*dy/dx;wall=1;}
                else if(tx>board.right){hx=board.right;hy=y+(board.right-x)*dy/dx;wall=1;}
                else if(ty<board.top){hy=board.top;hx=x+(board.top-y)*dx/dy;wall=2;}
                else if(ty>board.bottom){hy=board.bottom;hx=x+(board.bottom-y)*dx/dy;wall=2;}

                if(wall==0){path.lineTo(tx,ty);break;}
                path.lineTo(hx,hy);
                if(!cushions || ++hits>=3)break;
                if(wall==1)dx=-dx;else dy=-dy;
                x=hx;y=hy;remaining*=.68f;
            }

            stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeWidth(dp(2.5f));
            stroke.setColor(Color.argb(225,0,215,255));
            c.drawPath(path,stroke);

            // target direction marker
            p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);
            c.drawCircle(sx,sy,dp(13),p);
            p.setColor(Color.rgb(0,175,220));c.drawCircle(sx,sy,dp(6),p);

            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));
            p.setColor(Color.argb(120,255,255,255));
            c.drawCircle(board.centerX(),board.centerY(),size*.13f,p);
        }

        @Override public boolean onTouchEvent(android.view.MotionEvent e){
            if(e.getAction()==MotionEvent.ACTION_DOWN || e.getAction()==MotionEvent.ACTION_MOVE){
                if(board.contains(e.getX(),e.getY())){
                    strikerX=e.getX();strikerY=e.getY();invalidate();return true;
                }
            }
            return true;
        }
    }
}
