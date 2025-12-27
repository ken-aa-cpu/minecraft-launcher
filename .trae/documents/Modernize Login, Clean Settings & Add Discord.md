I will implement the requested changes to modernize the login flow, clean up the settings UI, and integrate Discord.

### 1. Modernize Login Flow (System Browser)

* **Target**: `com.mcserver.launcher.auth.MicrosoftAuthenticator`

* **Change**: Replace the `WebView` implementation with a system browser approach.

  * Start a temporary local HTTP server (using `HttpServer`) on a random available port.

  * Construct the OAuth2 authorization URL with `redirect_uri` pointing to `http://localhost:<port>/callback`.

  * Use `java.awt.Desktop` to open the system default browser.

  * Capture the authorization code via the local server, display a "Login Successful" message in the browser, and then close the server.

  * *Note*: This ensures a smoother login experience using the user's existing browser session.

### 2. Clean Up Settings UI

* **Target**: `src/main/resources/settings.fxml` & `com.mcserver.launcher.ui.SettingsController`

* **Remove**:

  * "Server Settings" section (Server Address, Port).

  * "Update Settings" section (GitHub Repo, Auto-update toggle).

  * "Theme" selection option.

* **Update**:

  * **Language Selection**: Limit options to "繁體中文" (Traditional Chinese), "日本語" (Japanese), and "English".

  * **Logic**: Remove corresponding data binding and save logic in `SettingsController.java` to prevent errors.

### 3. Discord Integration

* **Target**: `src/main/resources/launcher.fxml` & `com.mcserver.launcher.LauncherController`

* **Change**:

  * Bind the existing Discord button to the provided URL: `https://discord.gg/aQVjDNG5Fv`.

  * Implement the button handler to open the URL in the system browser.

### 4. Verification

* **Settings Check**: I will verify that critical settings like **Java Path** and **Memory Allocation** are still correctly saved and loaded after the cleanup.

* **Functionality**: Ensure the "Reset" button restores defaults correctly for the remaining valid settings.

