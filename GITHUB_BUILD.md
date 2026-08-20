# Build the APK on GitHub — no Android Studio required

1. Create a new GitHub repository.
2. Upload the **contents of this `CarromAimAssistant` folder** (not the outer ZIP folder).
3. Commit/push to `main`.
4. Open the repository's **Actions** tab.
5. Select **Build Android APK**.
6. Click **Run workflow** if it did not start automatically.
7. When it finishes, open the workflow run.
8. Under **Artifacts**, download `carrom-aim-assistant-debug-apk`.
9. Unzip the artifact and install `app-debug.apk` on your Android phone.

The workflow installs Android SDK platform 35/build-tools 35.0.0 on the GitHub-hosted runner and runs the Gradle Android build remotely.

If GitHub asks you to enable Actions, enable them for the repository.
