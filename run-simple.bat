@echo off
echo 啟動SimpleLauncher...
java -cp "target\classes;target\dependency\*" --module-path "javafx-sdk-17.0.2\lib" --add-modules javafx.controls,javafx.fxml com.mcserver.launcher.SimpleLauncher
pause