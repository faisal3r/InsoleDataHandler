import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class HeatmapPanel extends JPanel{
	private static final long serialVersionUID = 1L;
	final static int LEFT = 0;
	final static int RIGHT = 1;
	final int heatPointRadius = 50;
	HeatPoint[] heatpoints = new HeatPoint[12];
	JTextField[] imuFields = new JTextField[6];
	JLabel title = new JLabel();
	private int side;
	
	public HeatmapPanel(int side){
		super.setLayout(new GridBagLayout());
		this.side = side;
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 0;
		c.gridx = 1;
		c.gridwidth = 2;
		this.add(title,c);
		
		for(int i=0; i<heatpoints.length; i++){
			heatpoints[i] = new HeatPoint(heatPointRadius);
		}
		for(int i=0; i<6; i++){
			imuFields[i] = new JTextField();
		}
		
		c.gridwidth = 1;
		c.ipadx = heatPointRadius;
		c.ipady = heatPointRadius*2;
		
		//JComponents placement
		if(side == LEFT){
			c.gridy = 1;
			c.gridx = 3;
			this.add(heatpoints[0], c); //FSR_00
			c.gridx = 2;
			this.add(heatpoints[1], c); //FSR_01
			c.gridx = 1;
			this.add(heatpoints[2], c); //FSR_02
			c.gridx = 0;
			this.add(heatpoints[3], c); //FSR_03
			c.gridy = 2;
			c.gridx = 3;
			this.add(heatpoints[4], c); //FSR_04
			c.gridx = 2;
			this.add(heatpoints[5], c); //FSR_05
			c.gridx = 1;
			this.add(heatpoints[6], c); //FSR_06
			c.gridx = 0;
			this.add(heatpoints[7], c); //FSR_07
			c.gridy = 3;
			this.add(heatpoints[8], c); //FSR_08
			c.gridy = 4;
			c.gridx = 2;
			this.add(heatpoints[9], c); //FSR_09
			c.gridx = 1;
			this.add(heatpoints[10], c); //FSR_10
			c.gridx = 0;
			this.add(heatpoints[11], c); //FSR_11
		}
		else{ //side == RIGHT
			c.gridy = 1;
			c.gridx = 0;
			this.add(heatpoints[0], c); //FSR_00
			c.gridx = 1;
			this.add(heatpoints[1], c); //FSR_01
			c.gridx = 2;
			this.add(heatpoints[2], c); //FSR_02
			c.gridx = 3;
			this.add(heatpoints[3], c); //FSR_03
			c.gridy = 2;
			c.gridx = 0;
			this.add(heatpoints[4], c); //FSR_04
			c.gridx = 1;
			this.add(heatpoints[5], c); //FSR_05
			c.gridx = 2;
			this.add(heatpoints[6], c); //FSR_06
			c.gridx = 3;
			this.add(heatpoints[7], c); //FSR_07
			c.gridy = 3;
			this.add(heatpoints[8], c); //FSR_08
			c.gridy = 4;
			c.gridx = 1;
			this.add(heatpoints[9], c); //FSR_09
			c.gridx = 2;
			this.add(heatpoints[10], c); //FSR_10
			c.gridx = 3;
			this.add(heatpoints[11], c); //FSR_11
		}
		
		c.ipady = 0; 
		c.gridy = 5;
		c.gridx = 0;
		c.gridwidth = 2;
		this.add(new JLabel("GYR_X"),c);
		c.gridx = 1;
		this.add(imuFields[0],c);
		c.gridx = 0;
		
		c.gridy = 6;
		this.add(new JLabel("GYR_Y"),c);
		c.gridx = 1;
		this.add(imuFields[1],c);
		c.gridx = 0;
		
		c.gridy = 7;
		this.add(new JLabel("GYR_Z"),c);
		c.gridx = 1;
		this.add(imuFields[2],c);
		c.gridx = 0;
		
		c.gridy = 8;
		this.add(new JLabel("ACC_X"),c);
		c.gridx = 1;
		this.add(imuFields[3],c);
		c.gridx = 0;
		
		c.gridy = 9;
		this.add(new JLabel("ACC_Y"),c);
		c.gridx = 1;
		this.add(imuFields[4],c);
		c.gridx = 0;
		
		c.gridy = 10;
		this.add(new JLabel("ACC_Z"),c);
		c.gridx = 1;
		this.add(imuFields[5],c);
	}
	
	@Override
	protected void paintComponent(Graphics g){
		super.paintComponent(g);
		//this is where the background insole is drawn
		//currently, only lines are being drawn
		if(side==RIGHT){
			g.drawLine(0, 0, heatPointRadius*4, 0);
			g.drawLine(heatPointRadius*4, 0, heatPointRadius*4, heatPointRadius*8);
			g.drawLine(heatPointRadius*4, heatPointRadius*8, heatPointRadius*1, heatPointRadius*8);
			g.drawLine(heatPointRadius*1, heatPointRadius*8, 0, heatPointRadius*4);
			g.drawLine(0, heatPointRadius*4, 0, 0);
		}
		else{
			g.drawLine(0, 0, heatPointRadius*4, 0);
			g.drawLine(0, 0, 0, heatPointRadius*8);
			g.drawLine(0, heatPointRadius*8, heatPointRadius*3, heatPointRadius*8);
			g.drawLine(heatPointRadius*3, heatPointRadius*8, heatPointRadius*4, heatPointRadius*4);
			g.drawLine(heatPointRadius*4, heatPointRadius*4, heatPointRadius*4, 0);
		}
	}
}

class HeatPoint extends JComponent{
	private static final long serialVersionUID = 1L;
	private int x, y,		//center of HeatPoint: reference coordinates to draw ovals
				maxRadius,	//radius of the biggest oval when value is 100
				drawRadius, //variable radius for ovals
				value, 		//sensor value, determines all ovals' sizes
				radius_i;	//variable radius for ovals
    private final static Color colors[] = {
    		new Color(0x00, 0x00, 0x80),
    		new Color(0x00, 0x00, 0xE0),
            new Color(0x00, 0x00, 0xFD),
            new Color(0x00, 0x3D, 0xFF),
            new Color(0x00, 0x7E, 0xFB),
            new Color(0x00, 0xBD, 0xFB),
            new Color(0x00, 0xFE, 0xFD),
            new Color(0x20, 0xFE, 0xE0),
            new Color(0x40, 0xFE, 0xBE),
            new Color(0x80, 0xFF, 0x7E),
            new Color(0x9E, 0xFF, 0x60),
    		new Color(0xC0, 0xFF, 0x3D),
            new Color(0xE0, 0xFF, 0x1F),
            new Color(0xFD, 0xFE, 0x00),
            new Color(0xFF, 0xBD, 0x00),
            new Color(0xFF, 0x7E, 0x00),
            new Color(0xFF, 0x3E, 0x00),
            new Color(0xFF, 0x00, 0x00),
            new Color(0xE3, 0x00, 0x00),
            new Color(0xBD, 0x00, 0x00)};
    private final int step = 100/colors.length;//steps between each oval

    public HeatPoint(int radius){
    	this.maxRadius = radius;
    	this.x = maxRadius/2;
    	this.y = maxRadius/2;
    }
    
    public HeatPoint(int x, int y, int radius){
    	this.maxRadius = radius;
    	this.x = x;
    	this.y = y;
    }
    
    @Override
    public void paintComponent(Graphics g) {
    	super.paintComponent(g);
    	drawRadius = maxRadius*value/100; 
        
    	for(int i=0; i<(value/step); i++){
	    	g.setColor(colors[i]);
	    	radius_i = (int)(drawRadius*(1-(i*((double)step/100)))); //radius of this specific oval (i)
	    	g.fillOval(x-radius_i/2, y-radius_i/2, radius_i, radius_i);
    	}
    	g.setColor(Color.BLACK);
    	g.drawString(value+"", 0, maxRadius+10);
    }

    public void setValue(int value){
        this.value = value;
        this.repaint();
    }
}