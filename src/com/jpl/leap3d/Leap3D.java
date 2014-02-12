package com.jpl.leap3d;

import com.interactivemesh.jfx.importer.ImportException;
import com.interactivemesh.jfx.importer.tds.TdsModelImporter;
import com.leapmotion.leap.CircleGesture;
import com.leapmotion.leap.Controller;
import java.lang.reflect.Field;
import java.net.URL;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point3D;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 *
 * JavaFX 3D and Leap Motion, with JDK8
 * 
 * @author José Pereda - @JPeredaDnr
 * created on 12-feb-2014 17:10
 * 
 * See Leap Motion Controller and JavaFX: A new touch-less approach 
 * http://jperedadnr.blogspot.com.es/2013/06/leap-motion-controller-and-javafx-new.html
 * and JavaFX 3D and Leap Motion: a short space adventure
 * http://www.youtube.com/watch?v=TS5RvqDsEoU&feature=player_embedded
 */
public class Leap3D extends Application {
    
    static {
        /*
            Set path to 
            • libLeapJava.dylib (Mac)
            • LeapJava.dll (Windows 32/64)
            • libLeapJava.so (Linux)
        */
        System.setProperty( "java.library.path", System.getProperty("user.home")+"\\Documents\\LeapSDK\\lib\\x64" );
 
        /* Work Around
        * http://blog.cedarsoft.com/2010/11/setting-java-library-path-programmatically/
        */
        Field fieldSysPath = null;
        try {
            fieldSysPath = ClassLoader.class.getDeclaredField( "sys_paths" );
        } catch (NoSuchFieldException | SecurityException ex) {
            System.out.println("Error Security: "+ ex.getMessage());
        }
        if(fieldSysPath!=null){
            fieldSysPath.setAccessible( true );
            try {
                fieldSysPath.set( null, null );
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                System.out.println("Error Illegal Access: "+ ex.getMessage());
            }
        }
    }
    
    private final AnchorPane root=new AnchorPane();
    private final Rotate cameraXRotate = new Rotate(0,0,0,0,Rotate.X_AXIS);
    private final Rotate cameraYRotate = new Rotate(0,0,0,0,Rotate.Y_AXIS);
    private final Translate cameraPosition = new Translate(-300,-550,-700);
    private final SimpleLeapListener listener = new SimpleLeapListener();
    private final Controller leapController = new Controller();
    private final BooleanProperty bRotating=new SimpleBooleanProperty(false);
    private Timeline timeline=null;
    private double dragStartX, dragStartY, dragStartRotateX, dragStartRotateY;
    
    @Override
    public void start(Stage primaryStage) {
        final Scene scene = new Scene(root, 1280, 720, true);
        // Background: http://3.bp.blogspot.com/-ki1SVb8jsyM/UNGAspL35hI/AAAAAAAADik/ji0Ab09kXNU/s1600/earth_by_Moguviel.jpg
        scene.setFill(new ImagePattern(new Image(getClass().getResource("earth_by_Moguviel.jpg").toExternalForm()))); //

        primaryStage.setTitle("Hubble 3D model - JavaFX - Leap Motion");
        
        final Camera camera = new PerspectiveCamera();
        camera.getTransforms().addAll(cameraXRotate,cameraYRotate,cameraPosition);
        scene.setCamera(camera);
        leapController.addListener(listener);
 
        /*
        http://www.interactivemesh.org/models/jfx3dimporter.html
        by August Lammersdorf http://www.interactivemesh.org
        */
        TdsModelImporter model=new TdsModelImporter();
        try {
            /*
            3D Model: http://www.nasa.gov/multimedia/3d_resources/assets/HST_3DS.html
            'Hubble Space Telescope' (C) Copyright National Aeronautics and Space Administration (NASA). 
            */
            /*
            NASA Usage Guidelines:
            http://www.nasa.gov/audience/formedia/features/MP_Photo_Guidelines.html#.UvujpoXvjz8
            NASA still images; audio files; video; and computer files used in the rendition 
            of 3-dimensional models, such as texture maps and polygon data in any format, 
            generally are not copyrighted. You may use NASA imagery, video, audio, and 
            data files used for the rendition of 3-dimensional models for educational or 
            informational purposes, including photo collections, textbooks, public exhibits, 
            computer graphical simulations and Internet Web pages. This general permission 
            extends to personal Web pages.
            */
            URL hubbleUrl = this.getClass().getResource("hst.3ds");
            model.read(hubbleUrl);
        }
        catch (ImportException e) {
            System.out.println("Error importing 3ds model: "+e.getMessage());
            return;
        }
        final Node[] hubbleMesh = model.getImport();
        model.close();
        final Group model3D = new Group(hubbleMesh);
  
        final PointLight pointLight = new PointLight(Color.ANTIQUEWHITE);
        pointLight.setTranslateX(800);
        pointLight.setTranslateY(-800);
        pointLight.setTranslateZ(-1000);
        root.getChildren().addAll(model3D,pointLight);
        
        listener.posHandLeftProperty().addListener((ObservableValue<? extends Point3D> ov, Point3D t, final Point3D t1) -> {
            Platform.runLater(() -> {
                double z = listener.z1Property().getValue();
                model3D.setScaleX(1d+z/200d);
                model3D.setScaleY(1d+z/200d);
                model3D.setScaleZ(1d+z/200d);
                double roll=listener.rollLeftProperty().get();
                double pitch=-listener.pitchLeftProperty().get();
                double yaw=-listener.yawLeftProperty().get();
                matrixRotateNode(model3D,roll,pitch,yaw);
            });
        });
        
        listener.circleProperty().addListener((ObservableValue<? extends CircleGesture> ov, CircleGesture t, final CircleGesture t1) -> {
            if(t1.radius()>20 && t1.state().equals(CircleGesture.State.STATE_STOP)){
                if(!bRotating.getValue()){
                    Platform.runLater(() -> {
                        double d=360d;
                        if (t1.pointable().direction().angleTo(t1.normal()) <= Math.PI/4) {
                            d=-360d; // clockwise
                        }
                        timeline = new Timeline(
                                new KeyFrame(Duration.ZERO, new KeyValue(model3D.rotateProperty(),model3D.getRotate())),
                                new KeyFrame(Duration.seconds(6-t1.radius()/20), new KeyValue(model3D.rotateProperty(),model3D.getRotate()+d))
                        );
                        timeline.setCycleCount(Timeline.INDEFINITE);
                        timeline.play();
                        System.out.println("rotating "+d+" r: "+t1.radius());
                        bRotating.set(true);
                    });
                } else if(timeline!=null){
                    timeline.stop();
                    System.out.println("stop rotating");
                    bRotating.set(false);
                }
            }
        });
        
        scene.addEventHandler(MouseEvent.ANY, (MouseEvent event) -> {
            if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
                dragStartX = event.getSceneX();
                dragStartY = event.getSceneY();
                dragStartRotateX = cameraXRotate.getAngle();
                dragStartRotateY = cameraYRotate.getAngle();
            } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                double xDelta = event.getSceneX() -  dragStartX;
                double yDelta = event.getSceneY() -  dragStartY;
                cameraXRotate.setAngle(dragStartRotateX - (yDelta*0.7));
                cameraYRotate.setAngle(dragStartRotateY + (xDelta*0.7));
            }
        });
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private void matrixRotateNode(Node n, double alf, double bet, double gam){
        double A11=Math.cos(alf)*Math.cos(gam);
        double A12=Math.cos(bet)*Math.sin(alf)+Math.cos(alf)*Math.sin(bet)*Math.sin(gam);
        double A13=Math.sin(alf)*Math.sin(bet)-Math.cos(alf)*Math.cos(bet)*Math.sin(gam);
        double A21=-Math.cos(gam)*Math.sin(alf);
        double A22=Math.cos(alf)*Math.cos(bet)-Math.sin(alf)*Math.sin(bet)*Math.sin(gam);
        double A23=Math.cos(alf)*Math.sin(bet)+Math.cos(bet)*Math.sin(alf)*Math.sin(gam);
        double A31=Math.sin(gam);
        double A32=-Math.cos(gam)*Math.sin(bet);
        double A33=Math.cos(bet)*Math.cos(gam);
         
        double d = Math.acos((A11+A22+A33-1d)/2d);
        if(d!=0d){
            double den=2d*Math.sin(d);
            Point3D p= new Point3D((A32-A23)/den,(A13-A31)/den,(A21-A12)/den);
            n.setRotationAxis(p);
            n.setRotate(Math.toDegrees(d));                   
        }
    }

    @Override
    public void stop(){
        leapController.removeListener(listener);
    }
    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
