/* This is a java substitute for some qiv functions. */

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import java.io.*;
import java.awt.image.*;
import javax.imageio.ImageIO;


class MyFileChooser extends JFileChooser {
  MyFileChooser ( String path ) {
    super(path);
  }
  protected JDialog createDialog(Component parent) throws HeadlessException {
    JDialog dialog = super.createDialog(parent);
    dialog.setLocation(300, 200);
    dialog.setSize ( 1024, 768 );
    // dialog.setResizable(false);
    return dialog;
  }
}

class jiv_frame {
  public File f=null;
  public boolean valid=false;
  public BufferedImage image=null;

  jiv_frame ( File f, boolean load ) {
    this.f = f;
    this.valid = false;
    if ( load && (f != null) ) {
      try {
        this.image = ImageIO.read(f);
        this.valid = true;
      } catch (Exception oe) {
        this.image = null;
        this.valid = false;
        JOptionPane.showMessageDialog(null, "File error for: " + this.f, "File Path Error", JOptionPane.WARNING_MESSAGE);
      }
    }
  }

  public String toString() {
    return ( "" + this.f );
  }
}


public class jiv extends ZoomPanLib implements ActionListener, MouseMotionListener, MouseListener, KeyListener {

  JFrame parent_frame = null;

	static int w=800, h=600;

  boolean center_draw = false;
	boolean stroke_started = false;

  String current_directory = "";
  MyFileChooser file_chooser = null;

	ArrayList<jiv_frame> images = new ArrayList<jiv_frame>();  // Argument (if any) specifies initial capacity (default 10)
  int frame_index = -1;

	ArrayList strokes = new ArrayList();  // Argument (if any) specifies initial capacity (default 10)
	ArrayList stroke  = null;

	public void paint_frame (Graphics g) {
	  Dimension win_s = getSize();
	  int win_w = win_s.width;
	  int win_h = win_s.height;
	  if (recalculate) {
      set_scale_to_fit ( -100, 100, -100, 100, win_w, win_h );
	    recalculate = false;
	  }

    BufferedImage image_frame = null;

    if (images != null) {
      if (images.size() > 0) {
        if (frame_index < 0) frame_index = 0;
        if (frame_index >= images.size()) frame_index = images.size()-1;
        image_frame = images.get(frame_index).image;
      }
    }

		if (image_frame == null) {
		  System.out.println ( "Image is null" );
		} else {
		  // System.out.println ( "Image is NOT null" );
		  int img_w = image_frame.getWidth();
		  int img_h = image_frame.getHeight();
		  double img_wf = 200;
		  double img_hf = 200;
		  if (img_w >= img_h) {
		    // Make the image wider to fit
		    img_wf = img_w * img_wf / img_h;
		  } else {
		    // Make the height shorter to fit
		    img_hf = img_h * img_hf / img_w;
		  }
		  int draw_x = x_to_pxi(-img_wf/2.0);
		  int draw_y = y_to_pyi(-img_hf/2.0);
		  int draw_w = x_to_pxi(img_wf/2.0) - draw_x;
		  int draw_h = y_to_pyi(img_hf/2.0) - draw_y;
  		g.drawImage ( image_frame, draw_x, draw_y, draw_w, draw_h, this );
    }
	}


  //  MouseListener methods:
  

	Cursor current_cursor = null;
	Cursor b_cursor = null;
	int cursor_size = 33;

  public void mouseEntered ( MouseEvent e ) {
    if ( b_cursor == null ) {
      Toolkit tk = Toolkit.getDefaultToolkit();
      Graphics2D cg = null;
      BufferedImage cursor_image = null;
      Polygon p = null;
      int h = cursor_size;
      int w = cursor_size;

      // Create the move cursor

      int cw2 = w/2;   // Cursor width / 2
      int ch2 = h/2;   // Cursor height / 2

      int clw = w/12;  // Arrow line width / 2
      int caw = w/6;   // Arrow head width / 2
      int cal = (int)(w/3.5);   // Arrow head length

      p = new Polygon();
      p.addPoint (       -cw2,           0 );  // Left Point

      p.addPoint ( -(cw2-cal),         caw );  // Left arrow lower outside corner
      p.addPoint ( -(cw2-cal),         clw );  // Left arrow lower inside corner

      p.addPoint (       -clw,         clw );  // Left/Bottom corner

      p.addPoint (       -clw,     ch2-cal );  // Bottom arrow left inside corner
      p.addPoint (       -caw,     ch2-cal );  // Bottom arrow left outside corner

      p.addPoint (          0,         ch2 );  // Bottom Point

      p.addPoint (        caw,     ch2-cal );  // Bottom arrow right outside corner
      p.addPoint (        clw,     ch2-cal );  // Bottom arrow right inside corner

      p.addPoint (        clw,         clw );  // Right/Bottom corner

      p.addPoint (  (cw2-cal),         clw );  // Right arrow lower inside corner
      p.addPoint (  (cw2-cal),         caw );  // Right arrow lower outside corner

      p.addPoint (        cw2,           0 );  // Right Point

      p.addPoint (  (cw2-cal),        -caw );  // Right arrow upper outside corner
      p.addPoint (  (cw2-cal),        -clw );  // Right arrow upper inside corner

      p.addPoint (        clw,        -clw );  // Right/Top corner

      p.addPoint (        clw,  -(ch2-cal) );  // Top arrow right inside corner
      p.addPoint (        caw,  -(ch2-cal) );  // Top arrow right outside corner

      p.addPoint (          0,        -ch2 );  // Top Point

      p.addPoint (       -caw,  -(ch2-cal) );  // Top arrow left outside corner
      p.addPoint (       -clw,  -(ch2-cal) );  // Top arrow left inside corner

      p.addPoint (       -clw,        -clw );  // Left/Top corner

      p.addPoint ( -(cw2-cal),        -clw );  // Left arrow upper inside corner
      p.addPoint ( -(cw2-cal),        -caw );  // Left arrow upper outside corner

      p.addPoint (       -cw2,           0 );  // Left Point


      p.translate ( w/2, h/2 );

      cursor_image = new BufferedImage(cursor_size,cursor_size,BufferedImage.TYPE_4BYTE_ABGR);
      cg = cursor_image.createGraphics();
      cg.setColor ( new Color(255,255,255) );
      cg.fillPolygon ( p );
      cg.setColor ( new Color(0,0,0) );
      cg.drawPolygon ( p );

      b_cursor = tk.createCustomCursor ( cursor_image, new Point(cursor_size/2,cursor_size/2), "Both" );

    }
    if (current_cursor == null) {
      current_cursor = b_cursor;
      // current_cursor = Cursor.getPredefinedCursor ( Cursor.MOVE_CURSOR );
    }
    setCursor ( current_cursor );
  }

  public void mouseExited ( MouseEvent e ) {
    // System.out.println ( "Mouse exited" );
    super.mouseExited(e);
  }

  public void mouseClicked ( MouseEvent e ) {
    // System.out.println ( "Mouse clicked" );
    super.mouseClicked(e);
  }

  public void mousePressed ( MouseEvent e ) {
    // System.out.println ( "Mouse pressed" );
    super.mousePressed(e);
  }

  public void mouseReleased ( MouseEvent e ) {
    // System.out.println ( "Mouse released" );
    super.mouseReleased(e);
  }


  // MouseMotionListener methods:

  public void mouseDragged ( MouseEvent e ) {
    super.mouseDragged(e);
  }

  public void mouseMoved ( MouseEvent e ) {
    super.mouseMoved ( e );
  }


  // MouseWheelListener methods:
  public void mouseWheelMoved ( MouseWheelEvent e ) {
    /*
    if (e.isShiftDown()) System.out.println ( "Wheel Event with Shift" );
    if (e.isControlDown()) System.out.println ( "Wheel Event with Control" );
    if (e.isAltDown()) System.out.println ( "Wheel Event with Alt" );
    if (e.isMetaDown()) System.out.println ( "Wheel Event with Meta" );
    */
    if (images == null) {
      super.mouseWheelMoved ( e );
    } else {
      //if (modify_mode == true) {
      if (e.isShiftDown()) {
        if (images != null) {
          int scroll_wheel_delta = -e.getWheelRotation();
          frame_index += scroll_wheel_delta;
          if (frame_index < 0) frame_index = 0;
          if (frame_index >= images.size()) frame_index = images.size()-1;
          if (this.parent_frame != null) {
            this.parent_frame.setTitle ( "Section: " + (frame_index+1) );
          }
        }
      } else {
        super.mouseWheelMoved ( e );
      }
    }
    repaint();
  }


  JMenuItem move_menu_item = null;
  JMenuItem draw_menu_item = null;
  JMenuItem center_draw_menu_item = null;

  // KeyListener methods:

  public void keyTyped ( KeyEvent e ) {
    // System.out.println ( "Key: " + e );
    if (Character.toUpperCase(e.getKeyChar()) == ' ') {
      // Space bar toggles between drawing mode and move mode
    } else if (Character.toUpperCase(e.getKeyChar()) == 'P') {
    } else {
      // System.out.println ( "KeyEvent = " + e );
    }
    repaint();
  }
  public void keyPressed ( KeyEvent e ) {
    // System.out.println ( "Key Pressed, e = " + e );
    if ( (e.getKeyCode() == 33) || (e.getKeyCode() == 34) ) {
      // Figure out if there's anything to do
      if (images != null) {
        if (images.size() > 0) {
          int delta = 0;
          if (e.getKeyCode() == 33) {
            System.out.println ( "Page Up with " + images.size() + " frames" );
            delta = 1;
          } else if (e.getKeyCode() == 34) {
            System.out.println ( "Page Down with " + images.size() + " frames" );
            delta = -1;
          }
          if ((frame_index+delta >= 0) && (frame_index+delta < images.size())) {
            frame_index += delta;
            repaint();
          }
        }
      }
    }
    //super.keyPressed ( e );
  }
  public void keyReleased ( KeyEvent e ) {
    // System.out.println ( "Key Released" );
    //super.keyReleased ( e );
  }


  // ActionPerformed methods (mostly menu responses):

	public void actionPerformed(ActionEvent e) {
    Object action_source = e.getSource();

		String cmd = e.getActionCommand();
		System.out.println ( "ActionPerformed got \"" + cmd + "\"" );
		
		if (cmd.equalsIgnoreCase("Move")) {
      current_cursor = b_cursor;
      setCursor ( current_cursor );
		  stroke_started = false;
		  repaint();
		} else if (cmd.equalsIgnoreCase("Draw")) {
      current_cursor = Cursor.getPredefinedCursor ( Cursor.CROSSHAIR_CURSOR );
      if (center_draw) {
        current_cursor = Cursor.getPredefinedCursor ( Cursor.HAND_CURSOR );
      }
      setCursor ( current_cursor );
		  stroke_started = false;
		  repaint();
		} else if (cmd.equalsIgnoreCase("Clear")) {
	    strokes = new ArrayList();
	    stroke  = null;
	    repaint();
		} else if (cmd.equalsIgnoreCase("Print")) {
		  System.out.println ( "Images:" );
	    for (int i=0; i<this.images.size(); i++) {
        System.out.println ( "  " + this.images.get(i) );
      }
    } else if ( action_source == import_images_menu_item ) {
		  file_chooser.setMultiSelectionEnabled(true);
      FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", "jpg", "gif", "png", "tiff");
      file_chooser.setFileFilter(filter);
		  int returnVal = file_chooser.showDialog(this, "Image Files to Add");
		  if ( returnVal == JFileChooser.APPROVE_OPTION ) {
		    File selected_files[] = file_chooser.getSelectedFiles();
		    if (selected_files.length > 0) {
		      for (int i=0; i<selected_files.length; i++) {
            System.out.println ( "You chose this file: " + selected_files[i] );
            this.images.add ( new jiv_frame ( selected_files[i], true ) );
		      }
		      if (frame_index < 0) {
		        frame_index = 0;
		      }
	        repaint();
		    }
		  }
		} else if (cmd.equalsIgnoreCase("List Sections...")) {
		  System.out.println ( "Sections: ..." );
		} else if (cmd.equalsIgnoreCase("Exit")) {
			System.exit ( 0 );
		}
  }

  JMenuItem import_images_menu_item=null;

	public static void main ( String[] args ) {
	  for (int i=0; i<args.length; i++) {
		  System.out.println ( "Arg[" + i + "] = \"" + args[i] + "\"" );
		}
		System.out.println ( "jiv: Use the mouse wheel to zoom, and drag to pan." );
		javax.swing.SwingUtilities.invokeLater ( new Runnable() {
			public void run() {
			  JFrame f = new JFrame("jiv");
				f.setDefaultCloseOperation ( JFrame.EXIT_ON_CLOSE );
				
        jiv zp = new jiv();
        zp.parent_frame = f;
        /* Can't use args in here
        if (args.length > 0) {
          zp.current_directory = args[0];
        } else {
	        zp.current_directory = System.getProperty("user.dir");
        }
        */
        zp.current_directory = System.getProperty("user.dir");

				JMenuBar menu_bar = new JMenuBar();
          JMenuItem mi;

          JMenu series_menu = new JMenu("File");

            JMenu import_menu = new JMenu("Import");
              import_menu.add ( mi = zp.import_images_menu_item = new JMenuItem("Images...") );
              mi.addActionListener(zp);
            series_menu.add ( import_menu );

            series_menu.add ( mi = new JMenuItem("Print") );
            mi.addActionListener(zp);

            series_menu.addSeparator();

            series_menu.add ( mi = new JMenuItem("Exit") );
            mi.addActionListener(zp);

            menu_bar.add ( series_menu );


          JMenu help_menu = new JMenu("Help");
            help_menu.add ( mi = new JMenuItem("Manual...") );
            mi.addActionListener(zp);
            help_menu.add ( mi = new JMenuItem("Key commands...") );
            mi.addActionListener(zp);
            help_menu.add ( mi = new JMenuItem("Mouse clicks...") );
            mi.addActionListener(zp);
            help_menu.addSeparator();
            help_menu.add ( mi = new JMenuItem("License...") );
            mi.addActionListener(zp);
            help_menu.add ( mi = new JMenuItem("Version...") );
            mi.addActionListener(zp);
            menu_bar.add ( help_menu );


          JMenu mode_menu = new JMenu("Mode");
            mode_menu.add ( mi = new JMenuItem("Dump") );
            mi.addActionListener(zp);
            mode_menu.add ( mi = new JMenuItem("Clear") );
            mi.addActionListener(zp);
            menu_bar.add ( mode_menu );

				f.setJMenuBar ( menu_bar );
				
				zp.setBackground ( new Color (0,0,0) );
		    zp.file_chooser = new MyFileChooser ( zp.current_directory );

				f.add ( zp );
        zp.addKeyListener ( zp );
				f.pack();
				f.setSize ( w, h );
				f.setVisible ( true );
			  // Request the focus to make the drawing window responsive to keyboard commands without any clicking required
				zp.requestFocus();
			}
		} );
	}

}
