package backtool.ui.main;

import backtool.AppConfig;
import backtool.ui.pane.AutoTranslatorPane;
import backtool.ui.pane.DeleteBuildPane;
import backtool.ui.pane.DeleteEmptyDirPane;
import backtool.ui.pane.DirComparePane;
import backtool.ui.pane.DirDuplicatePane;
import backtool.ui.pane.DirCrossDuplicatePane;
import backtool.ui.pane.DirVersionCheckPane;
import backtool.ui.pane.DirVersionGenPane;
import backtool.ui.pane.EnglishListeningPane;
import backtool.ui.pane.LocalConfigBackPane;
import backtool.ui.pane.ScriptExecutorPane;
import backtool.util.StringConst;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import backtool.ui.component.FxKit;

public class BackToolApplication extends Application {

    private static BorderPane rootPane;

    private static final Logger exceptionLogger = Logger.getLogger(BackToolApplication.class.getName());

    @Override
    public void start(Stage primaryStage) {
        rootPane = new BorderPane();
        rootPane.setStyle("-fx-font-size:  16px");

        PbMenuBar topMenuBar = new PbMenuBar();
        VBox leftBar = buildToolBar(topMenuBar);
        rootPane.setTop(topMenuBar);
        rootPane.setLeft(leftBar);
        this.setCenterWithScrollPane(new DirVersionCheckPane());
        rootPane.setPadding(new Insets(0, 0, 0, 5));
        Scene scene = new Scene(rootPane, FxKit.width, FxKit.height);
        primaryStage.setMaximized(false);
        primaryStage.setTitle("文件备份小助手" + AppConfig.version);
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void setCenterWithScrollPane(Node content) {
        BackToolApplication.rootPane.setCenter(new ScrollPane(content));
    }

    public static void setCenter(Node content) {
        BackToolApplication.rootPane.setCenter(content);
    }

    protected class PbMenuBar extends MenuBar {

        public PbMenuBar() {
            this.setStyle("-fx-font-size:  20px");

            // 2 文件对比
            Menu m2 = new Menu("文件对比");
            m2.getItems().add(new MyMenuItem("计算MD5资产清单",
                    e -> setCenterWithScrollPane(new DirVersionGenPane()), true));
            m2.getItems().add(new MyMenuItem("检查资产清单",
                    e -> setCenterWithScrollPane(new DirVersionCheckPane()), true));
            m2.getItems().add(new MyMenuItem("按路径对比两个文件夹",
                    e -> setCenterWithScrollPane(new DirComparePane()), true));
            m2.getItems().add(new MyMenuItem("Md5-文件名对比两个文件夹",
                    e -> setCenterWithScrollPane(new DirCrossDuplicatePane()), true));
            this.getMenus().add(m2);


            // 1 删除重复文件
            Menu m1 = new Menu("删除重复文件");
            m1.getItems().add(new MyMenuItem("清空空文件夹",
                    e -> setCenterWithScrollPane(new DeleteEmptyDirPane()), true));
            m1.getItems().add(new MyMenuItem("删除构建产物",
                    e -> setCenterWithScrollPane(new DeleteBuildPane()), true));
            m1.getItems().add(new MyMenuItem("删除文件夹中的重复文件",
                    e -> setCenterWithScrollPane(new DirDuplicatePane()), true));

            this.getMenus().add(m1);


            // 4 命令工具
            Menu m4 = new Menu("命令工具");
            m4.getItems().add(new MyMenuItem("一键脚本执行器",
                    e -> setCenterWithScrollPane(new ScriptExecutorPane()), true));
            m4.getItems().add(new MyMenuItem("翻译",
                    e -> setCenterWithScrollPane(new AutoTranslatorPane()), true));
            m4.getItems().add(new MyMenuItem("英语听力格式转换",
                    e -> setCenterWithScrollPane(new EnglishListeningPane()), true));
            this.getMenus().add(m4);

            // 5 系统
            Menu m5 = new Menu("系统");
            m5.getItems().add(new MyMenuItem("系统配置",
                    e -> setCenterWithScrollPane(new LocalConfigBackPane()), false));
            m5.getItems().add(new MyMenuItem("查看系统版本", e -> viewSystemInfo(), false));
            m5.getItems().add(new SeparatorMenuItem());
            m5.getItems().add(new MyMenuItem("Exit", e -> Platform.exit(), false));
            this.getMenus().add(m5);
        }

        private void viewSystemInfo() {
            StringBuilder info = new StringBuilder();
            info.append("version:").append(AppConfig.version).append(StringConst.LF)
                    .append("author:dozr83").append(StringConst.LF);
            FxKit.alert(info.toString());
        }


        protected class MyMenuItem extends MenuItem {
            protected Consumer<ActionEvent> consumer;
            protected boolean addToolBar;

            public MyMenuItem(String text, Consumer<ActionEvent> consumer) {
                super(text);
                this.setText(text);
                this.consumer = consumer;
                this.setOnAction(event -> {
                    try {
                        consumer.accept(event);
                    } catch (Exception e) {
                        exceptionLogger.log(Level.SEVERE, "菜单项执行异常: " + text, e);
                        FxKit.alert("执行失败: " + e.getMessage());
                    }
                });
            }

            public MyMenuItem(String text, Consumer<ActionEvent> consumer, boolean addToolBar) {
                this(text, consumer);
                this.addToolBar = addToolBar;
            }
        }
    }

    public VBox buildToolBar(PbMenuBar pbMenuBar) {
        VBox leftBar = new VBox();
        leftBar.setSpacing(20);
        leftBar.setPadding(new Insets(20, 5, 5, 5));
        final AtomicInteger maxLength = new AtomicInteger(-1);
        pbMenuBar.getMenus().forEach(menu -> {
            menu.getItems().forEach(menuItem -> {
                if (menuItem instanceof PbMenuBar.MyMenuItem) {
                    PbMenuBar.MyMenuItem myMenuItem = (PbMenuBar.MyMenuItem) menuItem;
                    if (myMenuItem.addToolBar) {
                        if (menuItem.getText().length() > maxLength.get()) {
                            maxLength.set(menuItem.getText().length());
                        }
                    }

                }
            });
        });
        pbMenuBar.getMenus().forEach(menu -> {
            menu.getItems().forEach(menuItem -> {
                if (menuItem instanceof PbMenuBar.MyMenuItem) {
                    PbMenuBar.MyMenuItem myMenuItem = (PbMenuBar.MyMenuItem) menuItem;
                    if (myMenuItem.addToolBar) {
                        String text = myMenuItem.getText();
                        if (text.length() < maxLength.get()) {
                            int all = maxLength.get() - text.length();
                            int left = all / 2;
                            int right = all - left;
                            text = "　".repeat(left) + text + "　".repeat(right);
                        }
                        FxKit.FButton button = new FxKit.FButton(text, e -> myMenuItem.consumer.accept(e));
                        button.setMinWidth(200);
                        leftBar.getChildren().add(button);
                    }
                }
            });
        });
        return leftBar;
    }

}
