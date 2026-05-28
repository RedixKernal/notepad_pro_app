package com.ravi.notesapp.util;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class ResizeHelper {

    public static void addResizeListener(Stage stage) {
        ResizeListener resizeListener = new ResizeListener(stage);
        stage.getScene().addEventHandler(MouseEvent.MOUSE_MOVED, resizeListener);
        stage.getScene().addEventHandler(MouseEvent.MOUSE_PRESSED, resizeListener);
        stage.getScene().addEventHandler(MouseEvent.MOUSE_DRAGGED, resizeListener);
        stage.getScene().addEventHandler(MouseEvent.MOUSE_EXITED, resizeListener);
        stage.getScene().addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, resizeListener);
        
        ObservableList<Node> children = stage.getScene().getRoot().getChildrenUnmodifiable();
        for (Node child : children) {
            addListenerDeeply(child, resizeListener);
        }
    }

    private static void addListenerDeeply(Node node, EventHandler<MouseEvent> listener) {
        node.addEventHandler(MouseEvent.MOUSE_MOVED, listener);
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, listener);
        node.addEventHandler(MouseEvent.MOUSE_DRAGGED, listener);
        node.addEventHandler(MouseEvent.MOUSE_EXITED, listener);
        node.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, listener);
        if (node instanceof Parent) {
            Parent parent = (Parent) node;
            ObservableList<Node> children = parent.getChildrenUnmodifiable();
            for (Node child : children) {
                addListenerDeeply(child, listener);
            }
        }
    }

    static class ResizeListener implements EventHandler<MouseEvent> {
        private final Stage stage;
        private Cursor cursorEvent = Cursor.DEFAULT;
        private int border = 5;
        private double startX = 0;
        private double startY = 0;
        private double startWidth = 0;
        private double startHeight = 0;
        private double startScreenX = 0;
        private double startScreenY = 0;

        public ResizeListener(Stage stage) {
            this.stage = stage;
        }

        @Override
        public void handle(MouseEvent mouseEvent) {
            EventType<? extends MouseEvent> mouseEventType = mouseEvent.getEventType();
            Scene scene = stage.getScene();

            double mouseEventX = mouseEvent.getSceneX(), 
                   mouseEventY = mouseEvent.getSceneY(),
                   sceneWidth = scene.getWidth(),
                   sceneHeight = scene.getHeight();

            if (MouseEvent.MOUSE_MOVED.equals(mouseEventType) == true) {
                if (stage.isMaximized()) {
                    cursorEvent = Cursor.DEFAULT;
                    scene.setCursor(Cursor.DEFAULT);
                    return;
                }
                
                if (mouseEventX < border && mouseEventY < border) {
                    cursorEvent = Cursor.NW_RESIZE;
                } else if (mouseEventX < border && mouseEventY > sceneHeight - border) {
                    cursorEvent = Cursor.SW_RESIZE;
                } else if (mouseEventX > sceneWidth - border && mouseEventY < border) {
                    cursorEvent = Cursor.NE_RESIZE;
                } else if (mouseEventX > sceneWidth - border && mouseEventY > sceneHeight - border) {
                    cursorEvent = Cursor.SE_RESIZE;
                } else if (mouseEventX < border) {
                    cursorEvent = Cursor.W_RESIZE;
                } else if (mouseEventX > sceneWidth - border) {
                    cursorEvent = Cursor.E_RESIZE;
                } else if (mouseEventY < border) {
                    cursorEvent = Cursor.N_RESIZE;
                } else if (mouseEventY > sceneHeight - border) {
                    cursorEvent = Cursor.S_RESIZE;
                } else {
                    cursorEvent = Cursor.DEFAULT;
                }
                scene.setCursor(cursorEvent);
            } else if (MouseEvent.MOUSE_EXITED.equals(mouseEventType) || MouseEvent.MOUSE_EXITED_TARGET.equals(mouseEventType)) {
                scene.setCursor(Cursor.DEFAULT);
            } else if (MouseEvent.MOUSE_PRESSED.equals(mouseEventType) == true) {
                startX = stage.getX();
                startY = stage.getY();
                startWidth = stage.getWidth();
                startHeight = stage.getHeight();
                startScreenX = mouseEvent.getScreenX();
                startScreenY = mouseEvent.getScreenY();
            } else if (MouseEvent.MOUSE_DRAGGED.equals(mouseEventType) == true) {
                if (Cursor.DEFAULT.equals(cursorEvent) == false) {
                    if (Cursor.W_RESIZE.equals(cursorEvent) == false && Cursor.E_RESIZE.equals(cursorEvent) == false) {
                        double minHeight = stage.getMinHeight() > (border*2) ? stage.getMinHeight() : (border*2);
                        if (Cursor.NW_RESIZE.equals(cursorEvent) == true || Cursor.N_RESIZE.equals(cursorEvent) == true || Cursor.NE_RESIZE.equals(cursorEvent) == true) {
                            if (startHeight + startScreenY - mouseEvent.getScreenY() > minHeight) {
                                stage.setHeight(startHeight + startScreenY - mouseEvent.getScreenY());
                                stage.setY(startY - startScreenY + mouseEvent.getScreenY());
                            }
                        } else {
                            if (startHeight + mouseEvent.getScreenY() - startScreenY > minHeight) {
                                stage.setHeight(startHeight + mouseEvent.getScreenY() - startScreenY);
                            }
                        }
                    }

                    if (Cursor.N_RESIZE.equals(cursorEvent) == false && Cursor.S_RESIZE.equals(cursorEvent) == false) {
                        double minWidth = stage.getMinWidth() > (border*2) ? stage.getMinWidth() : (border*2);
                        if (Cursor.NW_RESIZE.equals(cursorEvent) == true || Cursor.W_RESIZE.equals(cursorEvent) == true || Cursor.SW_RESIZE.equals(cursorEvent) == true) {
                            if (startWidth + startScreenX - mouseEvent.getScreenX() > minWidth) {
                                stage.setWidth(startWidth + startScreenX - mouseEvent.getScreenX());
                                stage.setX(startX - startScreenX + mouseEvent.getScreenX());
                            }
                        } else {
                            if (startWidth + mouseEvent.getScreenX() - startScreenX > minWidth) {
                                stage.setWidth(startWidth + mouseEvent.getScreenX() - startScreenX);
                            }
                        }
                    }
                }
            }
        }
    }
}
