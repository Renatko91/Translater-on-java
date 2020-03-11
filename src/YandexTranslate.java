import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URL;
import java.text.*;
import java.util.*;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URLEncoder;
import java.util.Timer;

public class YandexTranslate extends Application {
    private static final String iconImageLoc = "http://icons.iconarchive.com/icons/chromatix/keyboard-keys/16/letter-e-icon.png";
    private Timer notificationTimer = new Timer();
    private DateFormat timeFormat = SimpleDateFormat.getTimeInstance();
    private static int i = 0;
    File file = new File("D:\\Словарь.txt");
    //public static Map<String, String> properties = new HashMap<>();
    public Properties prop = new Properties();
    boolean b = false;

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage stage) throws Exception {
        Platform.setImplicitExit(false);
        javax.swing.SwingUtilities.invokeLater(this::addAppToTray);

        GlobalScreen.registerNativeHook();
        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {}
            @Override
            public void nativeKeyReleased(NativeKeyEvent e) {
                if (e.getKeyCode() == NativeKeyEvent.VC_F12) {
                    Platform.runLater(() -> {
                        try {
                            windowTrans();
                        } catch (IOException | AWTException e1) {
                            e1.printStackTrace();
                        }
                    });
                }
            }
            @Override
            public void nativeKeyTyped(NativeKeyEvent e) {}
        });
    }

    private void addAppToTray() {
        try {
            java.awt.Toolkit.getDefaultToolkit();

            if (!java.awt.SystemTray.isSupported()) {
                System.out.println("No system tray support, application exiting.");
                Platform.exit();
            }

            java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
            URL imageLoc = new URL(
                    iconImageLoc
            );
            java.awt.Image image = ImageIO.read(imageLoc);
            java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(image);

            java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
            exitItem.addActionListener(event -> {
                Platform.setImplicitExit(true);
                try {
                    GlobalScreen.unregisterNativeHook();
                } catch (NativeHookException e) {
                    e.printStackTrace();
                }
                notificationTimer.cancel();
                Platform.exit();
                tray.remove(trayIcon);
            });

            final java.awt.PopupMenu popup = new java.awt.PopupMenu();
            popup.addSeparator();
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);
            notificationTimer.schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            javax.swing.SwingUtilities.invokeLater(() ->
                                    trayIcon.displayMessage(
                                            "",
                                            "",
                                            java.awt.TrayIcon.MessageType.INFO
                                    )
                            );
                        }
                    },
                    5_000,
                    60_000
            );

            tray.add(trayIcon);
        } catch (java.awt.AWTException | IOException e) {
            System.out.println("Unable to init system tray");
            e.printStackTrace();
        }
    }

    public void windowTrans() throws IOException, AWTException {
        String firstword = bufferCopy();
        String transword = translate("ru", bufferCopy());

        Stage parent = new Stage(StageStyle.TRANSPARENT);
        BorderPane root = new BorderPane();

        Scene scene = new Scene(root, 300, 50);

        root.setBackground(Background.EMPTY);

        HBox hboxTop = new HBox();
        HBox hboxBot = new HBox();

        Button buttonAdd = new Button("Add");
        Button buttonExit = new Button("Exit");

        root.setTop(hboxTop);
        root.setBottom(hboxBot);

        Label label = new Label(transword);
        label.setStyle("-fx-font-size: 10pt");

        hboxTop.getChildren().add(label);
        hboxBot.getChildren().addAll(buttonAdd,buttonExit);

        buttonAdd.setOnAction(event -> {
            parent.close();
            try {
                saveWord(file, firstword, transword);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        buttonExit.setOnAction(event -> {
            parent.close();
        });

        parent.setX(1570);
        parent.setY(950);
        parent.setAlwaysOnTop(true);
        parent.setScene(scene);
        parent.show();
    }

    public String bufferCopy() throws IOException, AWTException {
        Robot rb = new Robot();
        rb.keyPress(KeyEvent.VK_CONTROL);
        rb.keyPress(KeyEvent.VK_C);
        rb.keyRelease(KeyEvent.VK_CONTROL);
        rb.keyRelease(KeyEvent.VK_C);
        rb.keyPress(KeyEvent.VK_CONTROL);
        rb.keyPress(KeyEvent.VK_C);
        rb.keyRelease(KeyEvent.VK_CONTROL);
        rb.keyRelease(KeyEvent.VK_C);
        Clipboard cb = Clipboard.getSystemClipboard();
        return cb.getString();
    }

    private static String translate(String lang, String input) throws IOException {
        String urlStr = "https://translate.yandex.net/api/v1.5/tr.json/translate?key=trnsl.1.1.20200304T141915Z.9923c6baf303409b.e070c1a6ba7aee1364a48097440954ef846c7ffc";
        URL urlObj = new URL(urlStr);
        HttpsURLConnection connection = (HttpsURLConnection)urlObj.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
        dataOutputStream.writeBytes("text=" + URLEncoder.encode(input, "UTF-8") + "&lang=" + lang);

        InputStream response = connection.getInputStream();
        String json = new java.util.Scanner(response).nextLine();
        int start = json.indexOf("[");
        int end = json.indexOf("]");
        String translated = json.substring(start + 2, end - 1);
        i++;
        if (translated.equals(input) && i < 2) {
            return translate("en", input);
        } else return translated;
    }

    public void saveWord(File file, String firstword, String transword) throws IOException {
        FileReader fr = new FileReader(file);
        prop.load(fr);
        Set keys = prop.keySet();
        Iterator itr = keys.iterator();

        while(itr.hasNext()) {
            String n = (String)itr.next();
            if(firstword.equals(n)) {
                 b = true;
            }
        }
        if(!b) {
            try {
                FileWriter fw = new FileWriter(file, true);
                fw.write(firstword + " - " + transword);
                fw.write(10);
                fw.close();
            } catch (IOException e) {
                System.out.println("Ошибка программы");
            }
        } else {
            Stage mess = new Stage(StageStyle.TRANSPARENT);
            BorderPane bp2 = new BorderPane();
            Scene sc2 = new Scene(bp2, 100, 50);
            VBox vb = new VBox();
            Label label2 = new Label("Слово уже есть.");
            Button bok = new Button("OK");
            vb.getChildren().add(label2);
            vb.getChildren().add(bok);
            bok.setOnAction(event -> {
                mess.close();
            });
            bp2.setCenter(vb);
            mess.setScene(sc2);
            mess.show();
        }
        b = false;
    }
}