// Skeletal program for the "Model Earth" assignment
// Written by:  Minglun Gong
// Completed by: Ozair Shafiq

package modelearth;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;

// Main class
public class ModelEarth extends Frame implements ActionListener {
	View3DCanvas viewer;
	// Constructor
	public ModelEarth(double radius) {
		super("Earth Model");
		Panel controls = new Panel();
		Button button = new Button("Model Detail");
		button.addActionListener(this);
		controls.add(button);
		button = new Button("Less Detail");
		button.addActionListener(this);
		controls.add(button);
		button = new Button("Displacement");
		button.addActionListener(this);
		controls.add(button);
		button = new Button("Culling");
		button.addActionListener(this);
		controls.add(button);
		// add canvas and control panel
		viewer = new View3DCanvas(new Sphere(radius));
		add("Center", viewer);
		add("South", controls);
		addWindowListener(new ExitListener());
	}
	class ExitListener extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			System.exit(0);
		}
	}
	// Action listener for buttons
	public void actionPerformed(ActionEvent e) {
		if ( ((Button)e.getSource()).getLabel().equals("Model Detail") )
                {
			viewer.level ++;
                        viewer.sphere.increaseDetail();
                }
		else if ( ((Button)e.getSource()).getLabel().equals("Less Detail") )
                {
			viewer.level -= viewer.level > 0 ? 1 : 0;
                        viewer.sphere.init();
                        for (int i = 0; i < viewer.level; i++)
                            viewer.sphere.increaseDetail();
                }
		else if ( ((Button)e.getSource()).getLabel().equals("Displacement") )
                {
                    viewer.displace = ! viewer.displace;                    
                }
			
		else if ( ((Button)e.getSource()).getLabel().equals("Culling") )
			viewer.culling = ! viewer.culling;
		viewer.repaint();
	}
	public static void main(String[] args) {
		ModelEarth window = new ModelEarth(2);
		window.setSize(450, 550);
		window.setVisible(true);
	}
}

class View3DCanvas extends Canvas {
	// the sphere to display.
	Sphere sphere;
        // Earth Elevation image
        BufferedImage img;
	// pitch and paw angles for world to camera transform
	int pitch, yaw;
	// focal length and image size for camera to screen transform
	int focal, size;
	// canvas size for screen to raster transform
	int width, height, scale;
	// tessellation level
	int level;
	// whether displacement map or back face culling is enabled
	boolean displace, culling;
	// initialize the 3D view canvas
	public View3DCanvas(Sphere s) {
		sphere = s;
		focal = 4; size = 10;
                displace = false;
                culling = false;
                img = loadImage("./earth_elevation.png");
                
		DragListener drag = new DragListener();
		addMouseListener(drag);
		addMouseMotionListener(drag);
		addKeyListener(new ArrowListener());
		addComponentListener(new ResizeListener());
	}
        
        public Vertex getNormal(Vertex v, Vertex v2)
        {
            return new Vertex(v.y * v2.z - v.z * v2.y, 
                              v.z * v2.x - v.x * v2.z, 
                              v.x * v2.y - v.y * v2.x);
        }
        
        public double getDotProduct(Vertex v, Vertex v2)
        {
            return v.x * v2.x + v.y * v2.y + v.z * v2.z;
        }
        
        public double degreeToRadian(double angle)
        {
            return (angle * 3.14159) / 180.0;
        }
        
        public Vertex rotateAroundY(Vertex v)
        {
            double angle = degreeToRadian(yaw);
            //if (degreeToRadian(Math.abs(pitch) % 360) / 180.0 > 1)
            //    angle = -angle;
            return new Vertex(v.x * Math.cos(angle) - v.z * Math.sin(angle), 
                              v.y, 
                              v.x * Math.sin(angle) + v.z * Math.cos(angle));
        }
        
        public Vertex rotateAroundX(Vertex v)
        {
            double angle = degreeToRadian(pitch);
            return new Vertex(v.x, 
                              v.y * Math.cos(angle) + v.z * Math.sin(angle), 
                              v.z * Math.cos(angle) - v.y * Math.sin(angle));
        }
       
	// Demo transform between camera and screen coordinates (scaled parallel projection)
	public Vertex camera2Screen(Vertex from) {
               // return new Vertex(from.x/size*focal, from.y/size*focal, 0);
               return new Vertex((from.x * focal) / size, (from.y * focal) / size, 0);
	}
	// Transform between screen and raster coordinates
	public Vertex screen2Raster(Vertex from) {
		return new Vertex(from.x*scale+width/2, -from.y*scale+height/2, 0);
	}
        
        @Override
	public void paint(Graphics g) {
            // display current parameter values
            g.setColor(Color.blue);
            g.drawString("Pitch = "+pitch+", Yaw = "+yaw+", Focal = "+focal+", Size = "+size, 10, 20);
            g.drawString("Tessellation level : "+level, 10, 40);
            g.drawString("Displacement : "+(displace?"on":"off"), 10, 60);
            g.drawString("Culling : "+(culling?"on":"off"), 10, 80);
            // draw the triangle mesh obtained through sphere tessellation
            g.setColor(Color.black);
            Vertex V = new Vertex(0, 0, -focal);
            for ( Triangle t : sphere.triangles ) {                                                                  
                Vertex p0, p1, p2;

                Vertex t0 = rotateAroundX(rotateAroundY(sphere.vertices.get(t.v0)));
                Vertex t1 = rotateAroundX(rotateAroundY(sphere.vertices.get(t.v1)));
                Vertex t2 = rotateAroundX(rotateAroundY(sphere.vertices.get(t.v2)));
                             
                if (!culling || getDotProduct(V, getNormal(t0.subtract(t1), t0.subtract(t2))) < 0)
                {
                    p0 = screen2Raster(camera2Screen(t0));
                    p1 = screen2Raster(camera2Screen(t1));
                    p2 = screen2Raster(camera2Screen(t2));

                    g.drawLine((int)p0.x, (int)p0.y, (int)p1.x, (int)p1.y);
                    g.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
                    g.drawLine((int)p2.x, (int)p2.y, (int)p0.x, (int)p0.y);
                }
            }
            
            if (displace)
            {
                System.out.println("Height: " + img.getHeight());
                System.out.println("Width: " + img.getWidth());
                
                for (int i = 0; i < img.getWidth(); i++)
                {
                    for (int j = 0; j < img.getHeight(); j++)                       
                    {
                        //Color pixel = new Color(img.getRGB(i, j));
                        //System.out.print(pixel.getAlpha() + " ");
                        //int pixel = img.getRGB(i, j);
                        //int v = (((pixel >> 16) & 0xFF) + ((pixel >> 8) & 0xFF) + (pixel & 0xFF)) / 3;
                        //System.out.print((v) + " ");
                        System.out.print((img.getRGB(i, j) & 0xFF) + " ");
                    }
                    System.out.print("\n");
                }
            }
	}
        
        public BufferedImage loadImage(String name) {
		Image image = Toolkit.getDefaultToolkit().getImage(name);
		MediaTracker mt = new MediaTracker(this);
		try {
			mt.addImage(image, 0);
			mt.waitForID(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// convert to buffered image
		width = image.getWidth(null);
		height = image.getHeight(null);
		BufferedImage buff = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		buff.createGraphics().drawImage(image, 0, 0, null);
		return buff;
	}

	// Resize listener for updating canvas size
	class ResizeListener extends ComponentAdapter {
                @Override
		public void componentResized(ComponentEvent e) {
			width = getWidth();
			height = getHeight();
			scale = Math.min(width/2, height/2);
		}
	}
	// Action listener for mouse
	class DragListener extends MouseAdapter implements MouseMotionListener {
		int lastX, lastY;
                @Override
		public void mousePressed(MouseEvent e) {
			lastX = e.getX();
			lastY = e.getY();
		}
		// update pitch and yaw angles when the mouse is dragged.
                @Override
		public void mouseDragged(MouseEvent e) {
			yaw -= e.getX() - lastX;
			pitch -= e.getY() - lastY;
			lastX = e.getX();
			lastY = e.getY();
			repaint();
		}
	}
	// Action listener for keyboard
	class ArrowListener extends KeyAdapter {
                @Override
		public void keyPressed(KeyEvent e) {
			if ( e.getKeyCode() == e.VK_DOWN && focal>3 )
				focal --;
			else if ( e.getKeyCode() == e.VK_UP && focal<20 )
				focal ++;
			else if ( e.getKeyCode() == e.VK_LEFT && size>1 )
				size --;
			else if ( e.getKeyCode() == e.VK_RIGHT && size<20 )
				size ++;
			repaint();
		}
	}
}

// Vertex definition: coordinates for a 3D point
class Vertex {
	double x, y, z;
	// constructors
	public Vertex(double a, double b, double c) {
		x = a; y = b; z = c;
	}
        
        public Vertex subtract(Vertex V)
        {
            return new Vertex(V.x - x, V.y - y, V.z - z);
        }
        
        public Vertex add(Vertex V)
        {
            return new Vertex((V.x + x), (V.y + y), (V.z + z));
        }
        
        public Vertex mid(Vertex V)
        {
            return new Vertex((V.x + x) * 0.5, (V.y + y) * 0.5, (V.z + z) * 0.5);           
        }
        
        public double length()
        {
            return Math.sqrt(x * x + y * y + z * z);
        }
        
        public Vertex normalizeToLength(double l)
        {
            double ThisLength = this.length();
            return new Vertex(x * (l / ThisLength), y * (l / ThisLength), z * (l / ThisLength));           
        }
}

// Triangle definition: indices for 3 vertices
class Triangle {
	int v0, v1, v2;
	public Triangle(int u, int v, int w) {
		v0 = u; v1 = v; v2 = w;
	}
}

// Sphere definition
class Sphere {
	double radius;
        ArrayList<Vertex> vertices;
        ArrayList<Triangle> triangles;

	public Sphere(double r) {
		radius = r;

		init();                    
	}
        
        public void init()
        {
            // Generate triangle mesh for Octahedron - start point for tessellation		
            vertices = new ArrayList<>();
            vertices.add(new Vertex(0, 0, radius));
            vertices.add(new Vertex(radius, 0, 0));
            vertices.add(new Vertex(0, radius, 0));
            vertices.add(new Vertex(-radius, 0, 0));
            vertices.add(new Vertex(0, -radius, 0));
            vertices.add(new Vertex(0, 0, -radius));		

            triangles = new ArrayList<>();

            triangles.add(new Triangle(0, 1, 2));
            triangles.add(new Triangle(0, 2, 3));
            triangles.add(new Triangle(0, 3, 4));
            triangles.add(new Triangle(0, 4, 1));
            triangles.add(new Triangle(5, 2, 1));
            triangles.add(new Triangle(5, 3, 2));
            triangles.add(new Triangle(5, 4, 3));
            triangles.add(new Triangle(5, 1, 4));   
        }
        
        public void increaseDetail()
        {          
            ArrayList<Triangle> NewTriangles = new ArrayList<>();
                                         
            for (int i = 0; i < triangles.size(); i++)
            {
                Vertex v12, v23, v31, v1 = vertices.get(triangles.get(i).v0), v2 = vertices.get(triangles.get(i).v1), v3 = vertices.get(triangles.get(i).v2);
                                
                v12 = v1.mid(v2);
                v23 = v2.mid(v3);
                v31 = v3.mid(v1);
                                                           
                int FinalIndex = vertices.size();
                
                vertices.add(v12.normalizeToLength(radius));
                vertices.add(v23.normalizeToLength(radius));
                vertices.add(v31.normalizeToLength(radius));
                         
                int IndexV1 = triangles.get(i).v0,
                    IndexV2 = triangles.get(i).v1,
                    IndexV3 = triangles.get(i).v2,
                    IndexV12 = FinalIndex,
                    IndexV23 = FinalIndex + 1,
                    IndexV31 = FinalIndex + 2;
                                                                                   
                NewTriangles.add(new Triangle(IndexV1, IndexV12, IndexV31));
                NewTriangles.add(new Triangle(IndexV2, IndexV23, IndexV12));
                NewTriangles.add(new Triangle(IndexV3, IndexV31, IndexV23));
                NewTriangles.add(new Triangle(IndexV12, IndexV23, IndexV31));                             
            }
            
            triangles = NewTriangles;
        }          
}
