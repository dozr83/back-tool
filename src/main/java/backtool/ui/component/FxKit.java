package backtool.ui.component;

import backtool.util.StringConst;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import backtool.util.StringUtil;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FxKit {
    public static Insets DEFAULT_INSERT = new Insets(20, 5, 5, 5);
    public static double width;
    public static double height;
    //适用于文件输入框，长文本输入框等需要一定长度的输入
    public static double inputLength;
    //适用于输入数字等短输入场景
    public static double shortInputWidth = 150;
    public static double contentLength;
    public static double spacing = 10;
    public static final int image_width = 200;
    public static final int image_height = 200;

    // 日志对象
    private static final Logger logger = Logger.getLogger(FxKit.class.getName());

    static {
        Rectangle2D screenRectangle = Screen.getPrimary().getBounds();
        FxKit.width = screenRectangle.getWidth();
        FxKit.height = screenRectangle.getHeight();
        FxKit.inputLength = FxKit.width / 3;
        FxKit.contentLength = FxKit.width / 2;
    }

    public static void openFile(String path) {
        openFile(new File(path));
    }

    public static void openFile(File file) {
        try {
            if (file.isFile())
                file = file.getParentFile();
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "打开文件失败: " + file.getAbsolutePath(), e);
        }
    }

    public static TextField getNumberInputTextField(Integer count) {
        TextField countFiled = new TextField("" + count);
        countFiled.setMaxWidth(FxKit.shortInputWidth);
        return countFiled;
    }

    public static ImageView imageView(File file, int w, int h) {
        javafx.scene.image.Image image = null;
        try {
            image = new Image(new FileInputStream(file), w, h, true, true);
        } catch (FileNotFoundException e) {
            logger.severe("图片文件不存在: " + file.getAbsolutePath());
        }
        Tooltip tooltip = new Tooltip(file.getAbsolutePath());
        ImageView imageView = new ImageView(image);
        Tooltip.install(imageView, tooltip);
//        tooltip.setGraphic(imageView);
        return imageView;
    }

    public static Label getWrapLabel(String text, int maxWidth) {
        Label label = new Label(text);
        label.setMaxWidth(maxWidth);
        label.setWrapText(true);
        return label;
    }

    public static HBox getHBox(Node... children) {
        HBox hBox = new HBox(children);
        hBox.setSpacing(FxKit.spacing);
        hBox.setAlignment(Pos.CENTER_LEFT);
        return hBox;
    }

    public static VBox getVBox(Node... children) {
        VBox vBox = new VBox(children);
        vBox.setSpacing(FxKit.spacing);
        return vBox;
    }

    public static class LogTextArea extends TextArea {
        private final List<String> msgs = new ArrayList<>();
        // 日志对象
        private static final Logger log = Logger.getLogger(LogTextArea.class.getName());

        public LogTextArea(String result) {
            this.setPrefWidth(contentLength);
            this.setMinWidth(contentLength);  // 设置最小宽度
            this.setPadding(DEFAULT_INSERT);
            this.setText(result);
            this.setMinHeight(400);
        }

        public LogTextArea() {
            this("");
        }

        public void append(String result) {
            System.out.println(result);
            // 根据内容判断日志级别：错误信息用SEVERE，其他用INFO
            if (result != null && (result.toLowerCase().contains("error") || result.toLowerCase().contains("exception"))) {
                log.severe(result);
            } else {
                log.info(result);
            }
            while (msgs.size() > 100) {
                msgs.remove(0);
            }
            msgs.add(result);
            StringBuilder builder = new StringBuilder();
            msgs.forEach(msg -> builder.append(msg).append(StringConst.LF));
            this.setText(builder.toString());
        }

        public void clear() {
            msgs.clear();
            this.setText("");
        }
    }

    /**
     * Created by dozr83 on 2020/3/7.
     */
    public static class DirInputFx extends HBox {
        public TextField textField = new TextField();

        public DirInputFx() {
            this(null);
        }

        public DirInputFx(double prefWidth) {
            this(null);
            textField.setPrefWidth(prefWidth);
        }

        public DirInputFx(String dirValue) {
            this.setSpacing(spacing);
            textField.setEditable(false);
            textField.setPromptText("请选择文件夹");
            if (StringUtil.isNotBlank(dirValue)) {
                textField.setText(dirValue);
            }
            this.getChildren().addAll(textField, new FxKit.FButton("...", event -> select()), new FxKit.FButton("置空", event -> textField.setText("")));
        }

        private void select() {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("选择文件夹");
            File directory = directoryChooser.showDialog(new Stage());
            if (directory != null) {
                textField.setText(directory.getAbsolutePath());
            }
        }

        public String getSelectDir() {
            return textField.getText();
        }

        public void setSelectDir(String dirValue) {
            textField.setText(dirValue);
        }
    }

    /**
     * Created by dozr83 on 2020/3/7.
     */
    public static class FileInputFx extends HBox {
        private TextField textField = new TextField();

        public FileInputFx() {
            this(null);
        }

        public FileInputFx(double prefWidth) {
            this(null);
            textField.setPrefWidth(prefWidth);
        }

        public FileInputFx(String dirValue) {
            this.setSpacing(spacing);
            textField.setEditable(false);
            textField.setPromptText("请选择文件");
            if (StringUtil.isNotBlank(dirValue)) {
                textField.setText(dirValue);
            }
            this.getChildren().addAll(textField, new FxKit.FButton("...", event -> select()), new FxKit.FButton("置空", event -> textField.setText("")));
        }

        private void select() {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("请选择文件");
            File file = fileChooser.showOpenDialog(new Stage());
            if (file != null) {
                textField.setText(file.getAbsolutePath());
            }
        }

        public String getSelectFile() {
            return textField.getText();
        }

        public void setSelectFile(String dirValue) {
            textField.setText(dirValue);
        }
    }

    /**
     * Created by dozr83 on 2020/3/15.
     */
    public static class DirSelectPan extends GridPane {
        private List<DirInputFx> selectDirFxs = new ArrayList<>();

        public DirSelectPan(int rowCount, double prefWidth) {
            this.setHgap(5);
            this.setVgap(5);
            this.addRow(0, new Label("请选择文件夹"));
            for (int i = 0; i < rowCount; i++) {
                selectDirFxs.add(new DirInputFx(prefWidth));
                //添加删除按钮
                Button delButton = new FxKit.FButton("重置");
                final int index = i;
                delButton.setOnAction(event -> {
                    selectDirFxs.get(index).setSelectDir("");
                });
                this.addRow(i + 1, selectDirFxs.get(i), delButton);
            }
        }

        public void init(List<String> zipConfigs) {
            //全部内容置空，因为可能顺序不对，全删除，全设置
            for (int i = 0; i < selectDirFxs.size(); i++) {
                selectDirFxs.get(i).setSelectDir("");
            }
            //重置所有
            for (int i = 0; i < zipConfigs.size(); i++) {
                if (StringUtil.isNotBlank(zipConfigs.get(i))) {
                    selectDirFxs.get(i).setSelectDir(zipConfigs.get(i));
                }
            }
        }

        public List<String> getSelectDirs() {
            List<String> dirs = new ArrayList<>();
            for (int i = 0; i < selectDirFxs.size(); i++) {
                if (StringUtil.isNotBlank(selectDirFxs.get(i).getSelectDir())) {
                    dirs.add(selectDirFxs.get(i).getSelectDir());
                }
            }
            return dirs;
        }
    }

    public static class FGridPane extends GridPane {
        int index = 0;

        public FGridPane() {
            this.setAlignment(Pos.TOP_LEFT);
            this.setHgap(5);//控件间的垂直间隔
            this.setVgap(5);//控件间的水平间隔
            this.setPadding(new javafx.geometry.Insets(10, 5, 5, 10));
        }

        public void addRow(Node... children) {
            this.addRow(index++, children);
        }

        public void addMergeRow(Node child, int colspan) {
            this.add(child, 0, index++, colspan, 1);
        }

        public void clear() {
            this.getChildren().clear();
            index = 0;
        }
    }

    /**
     * Created by dozr83 on 2020/3/7.
     */
    public static class FButton extends Button {
        // 异常日志对象
        private static final Logger exceptionLogger = Logger.getLogger(FButton.class.getName());

        public FButton(String text, FButtonConsumer<ActionEvent> consumer) {
            super(text);
            this.setFont(Font.font(16));
            this.setOnAction(event -> {
                try {
                    consumer.accept(event);
                } catch (Exception e) {
                    exceptionLogger.log(Level.SEVERE, "按钮执行异常: " + text, e);
                    // 不再使用全局outputPane，由调用方自己处理
                }
            });
        }

        public FButton(String text) {
            super(text);
            this.setFont(Font.font(16));
        }
    }

    @FunctionalInterface
    public interface FButtonConsumer<T> {
        void accept(T t) throws Exception;
    }

    public static void alert(String headerText) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示框");
        alert.setHeaderText(headerText);
        alert.showAndWait();
    }

    public static class RadioPane<T> extends HBox {
        ToggleGroup versionTypeGroup = new ToggleGroup();
        RadioButton rb1 = new RadioButton();

        RadioButton rb2 = new RadioButton();

        public RadioPane(String text, String t1, T v1, String t2, T v2) {
            this.setSpacing(FxKit.spacing);
            rb1.setText(t1);
            rb1.setToggleGroup(versionTypeGroup);
            rb1.setUserData(v1);
            rb2.setText(t2);
            rb2.setToggleGroup(versionTypeGroup);
            rb2.setUserData(v2);
            this.getChildren().addAll(new Label(text), rb1, rb2);
        }

        public void select(T selectValue) {
            if (rb1.getUserData().equals(selectValue)) {
                rb1.setSelected(true);
            } else {
                rb2.setSelected(true);
            }
        }

        public T getSelectValue() {
            Toggle selectedToggle = versionTypeGroup.getSelectedToggle();
            T userData = (T) selectedToggle.getUserData();
            return userData;
        }
    }
}