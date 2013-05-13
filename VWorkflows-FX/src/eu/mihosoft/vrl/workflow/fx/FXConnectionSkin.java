/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.mihosoft.vrl.workflow.fx;

import eu.mihosoft.vrl.workflow.Connection;
import eu.mihosoft.vrl.workflow.ConnectionResult;
import eu.mihosoft.vrl.workflow.ConnectionSkin;
import eu.mihosoft.vrl.workflow.VFlow;
import eu.mihosoft.vrl.workflow.VNode;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;
import jfxtras.labs.scene.control.window.Window;
import jfxtras.labs.util.event.MouseControlUtil;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class FXConnectionSkin implements ConnectionSkin<Connection>, FXSkin<Connection, Path> {

    private ObjectProperty<VNode> senderProperty = new SimpleObjectProperty<>();
    private ObjectProperty<VNode> receiverProperty = new SimpleObjectProperty<>();
    private Path connectionPath;
    private LineTo lineTo;
    private MoveTo moveTo;
//    private Shape startConnector;
    private Shape receiverConnector;
    private Window receiverWindow;
    private VFlow controller;
    private Connection connection;
    private ObjectProperty<Connection> modelProperty = new SimpleObjectProperty<>();
    private ObjectProperty<Parent> parentProperty = new SimpleObjectProperty<>();
    private String type;
    private Node lastNode;
    private boolean valid = true;
//    private Window clipboard;
    private Window prevWindow;
    private FXSkinFactory skinFactory;

    public FXConnectionSkin(FXSkinFactory skinFactory, Parent parent, Connection connection, VFlow flow, String type) {
        setParent(parent);
        this.skinFactory = skinFactory;
        this.connection = connection;
        this.setModel(connection);
        this.controller = flow;
        this.type = type;

//        this.clipboard = clipboard;

//        startConnector = new Circle(20);
        receiverConnector = new Circle(20);

        moveTo = new MoveTo();
        lineTo = new LineTo();
        connectionPath = new Path(moveTo, lineTo);

        init();
    }

    private void init() {

        connectionPath.setFill(new Color(120.0 / 255.0, 140.0 / 255.0, 1, 0.2));
        connectionPath.setStroke(new Color(120 / 255.0, 140 / 255.0, 1, 0.42));
        connectionPath.setStrokeWidth(5);
        connectionPath.setStrokeLineCap(StrokeLineCap.ROUND);

//        receiverConnector.setFill(new Color(120.0 / 255.0, 140.0 / 255.0, 1, 0.2));
//        receiverConnector.setStroke(new Color(120 / 255.0, 140 / 255.0, 1, 0.42));
//        receiverConnector.setStrokeWidth(3);

        if (type.equals("control")) {
            receiverConnector.setFill(new Color(1.0, 1.0, 0.0, 0.75));
            receiverConnector.setStroke(new Color(120 / 255.0, 140 / 255.0, 1, 0.42));
        } else if (type.equals("data")) {
            receiverConnector.setFill(new Color(0.1, 0.1, 0.1, 0.5));
            receiverConnector.setStroke(new Color(120 / 255.0, 140 / 255.0, 1, 0.42));
        } else if (type.equals("event")) {
            receiverConnector.setFill(new Color(255.0 / 255.0, 100.0 / 255.0, 1, 0.5));
            receiverConnector.setStroke(new Color(120 / 255.0, 140 / 255.0, 1, 0.42));
        }

        receiverConnector.setStrokeWidth(3);

//        connectionPath.setStyle("-fx-background-color: rgba(120,140,255,0.2);-fx-border-color: rgba(120,140,255,0.42);-fx-border-width: 2;");
//        receiverConnector.setStyle("-fx-background-color: rgba(120,140,255,0.2);-fx-border-color: rgba(120,140,255,0.42);-fx-border-width: 2;");


//        final FlowNode sender = getController().getSender(connection);
//        final FlowNode receiver = getController().getReceiver(connection);

        final FXFlowNodeSkin senderSkin = (FXFlowNodeSkin) getController().getNodeSkinLookup().getById(skinFactory, connection.getSenderId());
        final Window senderWindow = senderSkin.getNode();

        FXFlowNodeSkin receiverSkin = (FXFlowNodeSkin) getController().getNodeSkinLookup().getById(skinFactory, connection.getReceiverId());
        receiverWindow = receiverSkin.getNode();

        addToClipboard();

        setSender(getController().getNodeLookup().getById(connection.getSenderId()));
        setReceiver(getController().getNodeLookup().getById(connection.getReceiverId()));


        DoubleBinding startXBinding = new DoubleBinding() {
            {
                super.bind(senderWindow.layoutXProperty(), senderWindow.widthProperty());
            }

            @Override
            protected double computeValue() {

                return senderWindow.getLayoutX() + senderWindow.getWidth();

            }
        };

        DoubleBinding startYBinding = new DoubleBinding() {
            {
                super.bind(senderWindow.layoutYProperty(), senderWindow.heightProperty(), receiverWindow.heightProperty());
            }

            @Override
            protected double computeValue() {
                return senderWindow.getLayoutY() + senderWindow.getHeight() / 2;
            }
        };

        final DoubleBinding receiveXBinding = new DoubleBinding() {
            {
                super.bind(receiverWindow.boundsInParentProperty());
            }

            @Override
            protected double computeValue() {

                Point2D location = NodeUtil.transformCoordinates(
                        receiverWindow.getBoundsInParent().getMinX(),
                        receiverWindow.getBoundsInParent().getMinY(), receiverWindow.getParent(), getParent());

                return location.getX();
            }
        };

        final DoubleBinding receiveYBinding = new DoubleBinding() {
            {
                super.bind(
                        receiverWindow.boundsInParentProperty(),
                        receiverWindow.heightProperty());
            }

            @Override
            protected double computeValue() {

                if (receiverWindow.getParent() == null) {
                    return 0;
                }

                if (receiverWindow.getParent() == getParent()) {
                    return receiverWindow.getLayoutY() + receiverWindow.getHeight() / 2;
                }

                Point2D location = NodeUtil.transformCoordinates(
                        0,
                        receiverWindow.getBoundsInParent().getMinY(),
                        receiverWindow.getParent(), getParent());

                double height =
                        receiverWindow.getBoundsInParent().getMaxY()
                        * receiverWindow.getParent().localToSceneTransformProperty().get().getMyy();

                return location.getY() + height / 2;
            }
        };

        receiverConnector.layoutXProperty().bind(receiveXBinding);
        receiverConnector.layoutYProperty().bind(receiveYBinding);

        moveTo.xProperty().bind(startXBinding);
        moveTo.yProperty().bind(startYBinding);

        lineTo.xProperty().bind(receiverConnector.layoutXProperty());
        lineTo.yProperty().bind(receiverConnector.layoutYProperty());

        receiverConnector.onMouseEnteredProperty().set(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                receiverConnector.toFront();
            }
        });

        receiverConnector.onMouseExitedProperty().set(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                if (!t.isPrimaryButtonDown()) {
//                    receiverConnector.toBack();
                }
            }
        });


        makeDraggable(receiveXBinding, receiveYBinding);

    }

    private void makeDraggable(
            final DoubleBinding receiveXBinding,
            final DoubleBinding receiveYBinding) {

        connectionPath.toFront();
        receiverConnector.toFront();

        MouseControlUtil.makeDraggable(receiverConnector, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {

//                Parent root = getParent().getScene().getRoot();

//                // TODO why can root be null?
//                if (root == null) {
//                    return;
//                }

                final Node n = NodeUtil.getDeepestNode(
                        getParent(),
                        t.getSceneX(), t.getSceneY(), FlowNodeWindow.class);

                if (lastNode != null) {
                    lastNode.setEffect(null);
                    lastNode = null;
                }

                valid = true;

                if (n != null) {
                    final FlowNodeWindow w = (FlowNodeWindow) n;

                    prevWindow = w;

                    ConnectionResult connResult =
                            getSender().getFlow().tryConnect(
                            getSender(), w.nodeSkinProperty().get().getModel(),
                            type);

                    if (connResult.getStatus().isCompatible()) {

                        DropShadow shadow = new DropShadow(20, Color.WHITE);
                        Glow effect = new Glow(0.5);
                        shadow.setInput(effect);
                        w.setEffect(shadow);

                        receiverConnector.setFill(new Color(220.0 / 255.0, 240.0 / 255.0, 1, 0.6));
                        valid = true;
                    } else {

                        DropShadow shadow = new DropShadow(20, Color.RED);
                        Glow effect = new Glow(0.8);
                        effect.setInput(shadow);
                        w.setEffect(effect);

                        receiverConnector.setFill(Color.RED);
                        valid = false;
                    }

                    lastNode = w;

                } else {
                    receiverConnector.setFill(new Color(120.0 / 255.0, 140.0 / 255.0, 1, 0.5));
                }
            }
        }, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                receiverConnector.layoutXProperty().unbind();
                receiverConnector.layoutYProperty().unbind();
            }
        });

        receiverConnector.layoutXProperty().bind(receiveXBinding);
        receiverConnector.layoutYProperty().bind(receiveYBinding);


        receiverConnector.onMouseReleasedProperty().set(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {

                if (lastNode != null) {
                    lastNode.setEffect(null);
                    lastNode = null;
                }

                if (!valid) {
                    init();
                    return;
                }

                receiverConnector.toBack();
                connectionPath.toBack();

                receiverConnector.layoutXProperty().bind(receiveXBinding);
                receiverConnector.layoutYProperty().bind(receiveYBinding);

//                receiverConnector.onMousePressedProperty().set(new EventHandler<MouseEvent>() {
//                    @Override
//                    public void handle(MouseEvent t) {
//                        makeDraggable(receiveXBinding, receiveYBinding);
//                    }
//                });



                Node n = NodeUtil.getDeepestNode(
                        getParent(),
                        t.getSceneX(), t.getSceneY(), FlowNodeWindow.class);

                if (n != null) {
                    connection.setReceiverId(
                            ((FlowNodeWindow) n).nodeSkinProperty().get().getModel().getId());

                    receiverConnector.setFill(new Color(120.0 / 255.0, 140.0 / 255.0, 1, 0.5));
                    init();

                } else {
                    remove();
                    connection.getConnections().remove(connection);
                }
            }
        });

    }

    @Override
    public VNode getSender() {
        return senderProperty.get();
    }

    @Override
    public final void setSender(VNode n) {
        senderProperty.set(n);
    }

    @Override
    public ObjectProperty<VNode> senderProperty() {
        return senderProperty;
    }

    @Override
    public VNode getReceiver() {
        return receiverProperty.get();
    }

    @Override
    public void setReceiver(VNode n) {
        receiverProperty.set(n);
    }

    @Override
    public ObjectProperty<VNode> receiverProperty() {
        return receiverProperty;
    }

    @Override
    public Path getNode() {
        return connectionPath;
    }

    @Override
    public Parent getContentNode() {
        return getParent();
    }

    @Override
    public final void setModel(Connection model) {
        modelProperty.set(model);
    }

    @Override
    public Connection getModel() {
        return modelProperty.get();
    }

    @Override
    public ObjectProperty<Connection> modelProperty() {
        return modelProperty;
    }

    final void setParent(Parent parent) {
        parentProperty.set(parent);
    }

    Parent getParent() {
        return parentProperty.get();
    }

    ObjectProperty<Parent> parentProperty() {
        return parentProperty;
    }

    @Override
    public void add() {
        NodeUtil.addToParent(getParent(), connectionPath);
//        VFXNodeUtils.addToParent(getParent(), startConnector);
        NodeUtil.addToParent(getParent(), receiverConnector);

//        startConnector.toBack();
        receiverConnector.toBack();
        connectionPath.toBack();
    }

    @Override
    public void remove() {
        if (connectionPath.getParent() == null || receiverConnector.getParent() == null) {
            return;
        }
        try {
            NodeUtil.removeFromParent(connectionPath);
            NodeUtil.removeFromParent(receiverConnector);
//            connection.getConnections().remove(connection);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @return the controller
     */
    @Override
    public VFlow getController() {
        return controller;
    }

    /**
     * @param controller the controller to set
     */
    @Override
    public void setController(VFlow controller) {
        this.controller = controller;
    }

    private void addToClipboard() {
//        if (!valid) {
//            clipboard.setVisible(true);
//            if (prevWindow != null) {
//                clipboard.toFront();
//                clipboard.setLayoutX(prevWindow.getLayoutX());
//                clipboard.setLayoutY(prevWindow.getLayoutY());
//
//                Timeline timeLine = new Timeline();
//
//                KeyValue vx1 = new KeyValue(clipboard.layoutXProperty(), clipboard.getLayoutX());
//                KeyValue vy1 = new KeyValue(clipboard.layoutYProperty(), clipboard.getLayoutY());
//                KeyValue vx2 = new KeyValue(clipboard.layoutXProperty(), prevWindow.getLayoutX());
//                KeyValue vy2 = new KeyValue(clipboard.layoutYProperty(), prevWindow.getLayoutY() - 100);
//
//                timeLine.getKeyFrames().add(new KeyFrame(Duration.ZERO, vx1, vy1));
//                timeLine.getKeyFrames().add(new KeyFrame(Duration.millis(300), vx2, vy2));
//
//                timeLine.play();
//
//                timeLine.statusProperty().addListener(new ChangeListener<Animation.Status>() {
//                    @Override
//                    public void changed(ObservableValue<? extends Animation.Status> ov, Animation.Status t, Animation.Status t1) {
//                        if (t1 == Animation.Status.STOPPED) {
//
//                            DoubleBinding clipboardYBinding = new DoubleBinding() {
//                                {
//                                    super.bind(prevWindow.layoutYProperty());
//                                }
//
//                                @Override
//                                protected double computeValue() {
//
//                                    return prevWindow.getLayoutY() - 100;
//                                }
//                            };
//
////                            clipboard.layoutXProperty().unbind();
////                            clipboard.layoutYProperty().unbind();
////
////                            clipboard.layoutXProperty().bind(prevWindow.layoutXProperty());
////                            clipboard.layoutYProperty().bind(clipboardYBinding);
//                        }
//                    }
//                });
//
//
//            }
//
//            receiverWindow = clipboard;
//        } else {
//            clipboard.setVisible(false);
//        }
    }

    /**
     * @return the skinFatory
     */
    @Override
    public FXSkinFactory getSkinFactory() {
        return skinFactory;
    }
}