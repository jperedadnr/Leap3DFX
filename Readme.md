JavaFX 3D and Leap Motion, with JDK8
 
See Leap Motion Controller and JavaFX: A new touch-less approach 
http://jperedadnr.blogspot.com.es/2013/06/leap-motion-controller-and-javafx-new.html

and JavaFX 3D and Leap Motion: a short space adventure
http://www.youtube.com/watch?v=TS5RvqDsEoU&feature=player_embedded


<b>Build</b>

1. Install last version of JDK8, from https://jdk8.java.net/download.html
2. Install last version of Leap Motion Software, from https://developer.leapmotion.com/downloads
3. Clone the project and open it on your favorite IDE. 
4. Before running this project don't forget to set the path of the Leap Motion library:

        • libLeapJava.dylib (Mac)
        • LeapJava.dll (Windows 32 or 64)
        • libLeapJava.so (Linux)

   at the beginning of Leap3D.java:

        System.setProperty( "java.library.path", "&lt;your path to Leap Motion library&gt;");

Jos&eacute; Pereda - @JPeredaDnr - Feb 2014