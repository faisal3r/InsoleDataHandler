import java.awt.Dimension;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLJPanel;

public class IMUVisualizerPanel extends GLJPanel implements GLEventListener{
	private static final long serialVersionUID = 1L;
	private float rotateX, rotateY, rotateZ;   //rotation amounts about axes
	private float avgX, avgY, avgZ;
	private float accelGyroRatio = 1f;
	private float filterRatio = 0.5f;
	private long currentTime, lastReadingTime;
	private float[] gyr = new float[3];
	private float[] acc = new float[3];
	private double pitch, roll;
	private double readRate;
	private static GLCapabilities capabilities = new GLCapabilities(null);
//boolean blue = false;
	
	public IMUVisualizerPanel() {
		super(capabilities);
		setPreferredSize( new Dimension(500,500) );
		addGLEventListener(this);
		lastReadingTime = System.currentTimeMillis();
	}

	// ------------ Cube control-------------------------------------
	public void repaint(float IMUReadings[]) {
		gyr[0] = IMUReadings[0];
		gyr[1] = IMUReadings[1];
		gyr[2] = IMUReadings[2];
		acc[0] = IMUReadings[3];
		acc[1] = IMUReadings[4];
		acc[2] = IMUReadings[5];
		pitch = Math.atan(acc[1]/Math.sqrt(Math.pow(acc[0],2)+Math.pow(acc[2],2)));
		roll = Math.atan(-acc[0]/acc[2]);
		
		//accelerometer control
		rotateX= (float)(pitch*180/Math.PI)*accelGyroRatio;
		rotateZ=-(float)(roll*180/Math.PI)*accelGyroRatio;
		
		//gyroscope control
		currentTime = System.currentTimeMillis();
		readRate = 1000/(double)(currentTime-lastReadingTime); //gyroscope readings are received in degrees per second
		rotateX+=(gyr[0]/readRate)*(1-accelGyroRatio);
		rotateZ-=(gyr[1]/readRate)*(1-accelGyroRatio);
		rotateY+=(gyr[2]/readRate)*(1-accelGyroRatio);
		lastReadingTime = currentTime;
		
		//Filter
		avgX = avgX*(1-filterRatio)+rotateX*filterRatio;
		avgY = avgY*(1-filterRatio)+rotateY*filterRatio;
		avgZ = avgZ*(1-filterRatio)+rotateZ*filterRatio;
		rotateX = avgX;
		rotateY = -avgY;
		rotateZ = -avgZ;
		
		repaint();
	}
	
	// ----------------- define some drawing methods for this program ----------
	
	private void shortRect(GL2 gl, float r, float g, float b){
		gl.glColor3f(r,g,b);         // The color for the square.
		gl.glTranslatef(0,0,0.8f);    // Move square 0.5 units forward.
		gl.glNormal3f(0,0,1);        // Normal vector to square (this is actually the default).
		gl.glBegin(GL2.GL_QUADS);
		gl.glVertex2f(-0.3f,-0.1f);    // Draw the square (before the
		gl.glVertex2f(0.3f,-0.1f);     //   the translation is applied)
		gl.glVertex2f(0.3f,0.1f);      //   on the xy-plane, with its
		gl.glVertex2f(-0.3f,0.1f);     //   at (0,0,0).
		gl.glEnd();
	}
	
	private void sideRect(GL2 gl, float r, float g, float b){
		gl.glColor3f(r,g,b);         // The color for the square.
		gl.glTranslatef(0,0,0.3f);    // Move square 0.5 units forward.
		gl.glNormal3f(0,0,1);        // Normal vector to square (this is actually the default).
		gl.glBegin(GL2.GL_QUADS);
		gl.glVertex2f(-0.8f,-0.1f);    // Draw the square (before the
		gl.glVertex2f(0.8f,-0.1f);     //   the translation is applied)
		gl.glVertex2f(0.8f,0.1f);      //   on the xy-plane, with its
		gl.glVertex2f(-0.8f,0.1f);     //   at (0,0,0).
		gl.glEnd();
	}
	
	private void topBottomRect(GL2 gl, float r, float g, float b){
		gl.glColor3f(r,g,b);         // The color for the square.
		gl.glTranslatef(0,0,0.1f);    // Move square 0.5 units forward.
		gl.glNormal3f(0,0,1);        // Normal vector to square (this is actually the default).
		gl.glBegin(GL2.GL_QUADS);
		gl.glVertex2f(-0.3f,-0.8f);    // Draw the square (before the
		gl.glVertex2f(0.3f,-0.8f);     //   the translation is applied)
		gl.glVertex2f(0.3f,0.8f);      //   on the xy-plane, with its
		gl.glVertex2f(-0.3f,0.8f);     //   at (0,0,0).
		gl.glEnd();
	}

	private void cube(GL2 gl) {
		gl.glPushMatrix();
		shortRect(gl,0,1,0);        // front face (Z) Green
		gl.glPopMatrix();

		gl.glPushMatrix();
		gl.glRotatef(180,0,1,0); // rotate square to back face (-Z)
		shortRect(gl,0,1,0.5f);     // Light Green
		gl.glPopMatrix();

		gl.glPushMatrix();
		gl.glRotatef(-90,0,1,0); // rotate square to left face (-X)
		sideRect(gl,0,1,1);        // Cyan
		gl.glPopMatrix();

		gl.glPushMatrix();
		gl.glRotatef(90,0,1,0); // rotate square to right face (X)
		sideRect(gl,0,0,1);       // Blue
		gl.glPopMatrix();

		gl.glPushMatrix();
		gl.glRotatef(-90,1,0,0); // rotate square to top face (Y)
		topBottomRect(gl,1,1,0);        // Yellow
		gl.glPopMatrix();

		gl.glPushMatrix();
		gl.glRotatef(90,1,0,0); // rotate square to bottom face (-Y)
		topBottomRect(gl,1,0.5f,0);    // Orange
		gl.glPopMatrix();
	}
	
	// ---------------  Methods of the GLEventListener interface -----------

	public void display(GLAutoDrawable drawable) {	
		// called when the panel needs to be drawn
		GL2 gl = drawable.getGL().getGL2();
		gl.glClearColor(0,0,0,0);
		gl.glClear( GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT );

		gl.glMatrixMode(GL2.GL_PROJECTION);  // Set up the projection.
		gl.glLoadIdentity();
		gl.glOrtho(-1,1,-1,1,-2,2);
		gl.glMatrixMode(GL2.GL_MODELVIEW);

		gl.glLoadIdentity();             // Set up modelview transform. 
		gl.glRotatef(rotateX,1,0,0);
		gl.glRotatef(rotateY,0,1,0);
		gl.glRotatef(rotateZ,0,0,1);

		cube(gl);
	}

	public void init(GLAutoDrawable drawable) {
		// called when the panel is created
		GL2 gl = drawable.getGL().getGL2();
		gl.glClearColor(0.8F, 0.8F, 0.8F, 1.0F);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_LIGHT0);
		gl.glEnable(GL2.GL_COLOR_MATERIAL);
	}

	public void dispose(GLAutoDrawable drawable) {
		// called when the panel is being disposed
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		// called when user resizes the window
	}

}