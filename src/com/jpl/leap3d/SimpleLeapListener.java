package com.jpl.leap3d;

import com.leapmotion.leap.CircleGesture;
import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Gesture;
import com.leapmotion.leap.Gesture.Type;
import com.leapmotion.leap.GestureList;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.Listener;
import com.leapmotion.leap.Screen;
import com.leapmotion.leap.Vector;
import java.util.LinkedList;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point3D;

/**
 *
 * @author José Pereda Llamas
 * Created on 10-abr-2013 - 19:58:55
 */
public class SimpleLeapListener extends Listener {

    private final DoubleProperty z1 = new SimpleDoubleProperty(0d);
    private final ObjectProperty<Point3D> posHandLeft=new SimpleObjectProperty<>();
    private final DoubleProperty pitchLeft=new SimpleDoubleProperty(0d);
    private final DoubleProperty rollLeft=new SimpleDoubleProperty(0d);
    private final DoubleProperty yawLeft=new SimpleDoubleProperty(0d);
    private final LimitQueue<Vector> posLeftAverage = new LimitQueue<>(30);
    private final LimitQueue<Double> pitchLeftAverage = new LimitQueue<>(30);
    private final LimitQueue<Double> rollLeftAverage = new LimitQueue<>(30);
    private final LimitQueue<Double> yawLeftAverage = new LimitQueue<>(30);
    private final ObjectProperty<CircleGesture> circle=new SimpleObjectProperty<>();
    private final IntegerProperty numHands=new SimpleIntegerProperty(0);
    
    public ObservableValue<CircleGesture> circleProperty(){ return circle; }
    public ObservableValue<Point3D> posHandLeftProperty(){ return posHandLeft; }
    public DoubleProperty yawLeftProperty(){ return yawLeft; }
    public DoubleProperty pitchLeftProperty(){ return pitchLeft; }
    public DoubleProperty rollLeftProperty(){ return rollLeft; }
    public DoubleProperty z1Property(){ return z1; }
    
    @Override
    public void onConnect(Controller controller) {
        controller.enableGesture(Gesture.Type.TYPE_CIRCLE);
    }
    
    @Override
    public void onFrame(Controller controller) {
        Frame frame = controller.frame();
        if (!frame.hands().isEmpty()) {
            numHands.set(frame.hands().count());
            Screen screen = controller.locatedScreens().get(0);
            if (screen != null && screen.isValid()){
                Hand hand;
                if(numHands.get()>1){
                    hand=frame.hands().leftmost();
                } else {
                    hand=frame.hands().get(0);
                }
                z1.set(hand.palmPosition().getZ());
                pitchLeftAverage.add(new Double(hand.direction().pitch()));
                rollLeftAverage.add(new Double(hand.palmNormal().roll()));
                yawLeftAverage.add(new Double(hand.direction().yaw()));                   
                pitchLeft.set(dAverage(pitchLeftAverage));
                rollLeft.set(dAverage(rollLeftAverage));
                yawLeft.set(dAverage(yawLeftAverage));

                Vector intersect = screen.intersect(hand.palmPosition(),hand.direction(), true);
                posLeftAverage.add(intersect);
                Vector avIntersect=vAverage(posLeftAverage);
                posHandLeft.setValue(new Point3D(screen.widthPixels()*Math.min(1d,Math.max(0d,avIntersect.getX())),
                        screen.heightPixels()*Math.min(1d,Math.max(0d,(1d-avIntersect.getY()))),
                        hand.palmPosition().getZ()));
            }               
        }
        
        GestureList gestures = frame.gestures();
        for (int i = 0; i < gestures.count(); i++) {
            Gesture gesture = gestures.get(i);
            if(gesture.type()==Type.TYPE_CIRCLE){
                CircleGesture cGesture = new CircleGesture(gesture);
                if(numHands.get()>1){
                    for(Hand h:cGesture.hands()){
                        if(h.equals(frame.hands().rightmost())){
                            circle.set(cGesture);
                            break;
                        }
                    }
                }
                break;
            }
        }
    }
    
    private Vector vAverage(LimitQueue<Vector> vectors){
        float vx=0f, vy=0f, vz=0f;
        for(Vector v:vectors){
            vx=vx+v.getX(); 
            vy=vy+v.getY(); 
            vz=vz+v.getZ();
        }
        return new Vector(vx/vectors.size(), vy/vectors.size(), vz/vectors.size());
    }
    
    private Double dAverage(LimitQueue<Double> vectors){
        double vx=0;
        for(Double d:vectors){
            vx=vx+d;
        }
        return vx/vectors.size();
    }
    
    private class LimitQueue<E> extends LinkedList<E> {
        private final int limit;
        public LimitQueue(int limit) {
            this.limit = limit;
        }

        @Override
        public boolean add(E o) {
            super.add(o);
            while (size() > limit) { super.remove(); }
            return true;
        }
    }
}
