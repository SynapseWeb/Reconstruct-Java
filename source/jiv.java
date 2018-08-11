/* This is a java substitute for some qiv functions. */

import javax.swing.*;
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


public class jiv extends ZoomPanLib implements ActionListener, MouseMotionListener, MouseListener, KeyListener {

  JFrame parent_frame = null;

	static int w=800, h=600;
	
	boolean drawing_mode = false;
  boolean center_draw = false;
	boolean stroke_started = false;
  BufferedImage image_frame = null;
  BufferedImage image_frames[] = null;
  int frame_index = -1;
  String current_directory = "";
  MyFileChooser file_chooser = null;

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
	Cursor h_cursor = null;
	Cursor v_cursor = null;
	Cursor b_cursor = null;
	int cursor_size = 33;

  public void mouseEntered ( MouseEvent e ) {
    if ( (h_cursor == null) || (v_cursor == null) || (b_cursor == null) ) {
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
    if (this.image_frames.length <= 0) {
      super.mouseWheelMoved ( e );
    } else {
      //if (modify_mode == true) {
      if (e.isShiftDown()) {
        // scroll_wheel_position += e.getWheelRotation();
        int scroll_wheel_delta = -e.getWheelRotation();
        frame_index += scroll_wheel_delta;
        if (this.parent_frame != null) {
          this.parent_frame.setTitle ( "Section: " + (frame_index+1) );
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
      if (this.image_frames.length > 1) {
        // Find the current index
        int current_index = -1;
        for (int i=0; i<this.image_frames.length; i++) {
          if (this.image_frame == this.image_frames[i]) {
            current_index = i;
            break;
          }
        }
        // System.out.println ( "Current image is " + current_index );
        int delta = 0;
        if (e.getKeyCode() == 33) {
          // System.out.println ( "Page Up with " + this.image_frames.length + " frames" );
          delta = 1;
        } else if (e.getKeyCode() == 34) {
          // System.out.println ( "Page Down with " + this.image_frames.length + " frames" );
          delta = -1;
        }
        if ((current_index+delta >= 0) && (current_index+delta < this.image_frames.length)) {
          this.image_frame = this.image_frames[current_index+delta];
          repaint();
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
      /*
        Alternative predefined cursors from java.awt.Cursor:
        current_cursor = Cursor.getPredefinedCursor ( Cursor.CROSSHAIR_CURSOR );
        current_cursor = Cursor.getPredefinedCursor ( Cursor.HAND_CURSOR );
        current_cursor = Cursor.getPredefinedCursor ( Cursor.MOVE_CURSOR );
      */
      setCursor ( current_cursor );
		  //center_draw = false;
		  drawing_mode = false;
		  stroke_started = false;
		  repaint();
		} else if (cmd.equalsIgnoreCase("Draw")) {
      current_cursor = Cursor.getPredefinedCursor ( Cursor.CROSSHAIR_CURSOR );
      if (center_draw) {
        current_cursor = Cursor.getPredefinedCursor ( Cursor.HAND_CURSOR );
      }
      setCursor ( current_cursor );
		  // center_draw = false;
		  drawing_mode = true;
		  stroke_started = false;
		  repaint();
		} else if (cmd.equalsIgnoreCase("Clear")) {
	    strokes = new ArrayList();
	    stroke  = null;
	    repaint();
    } else if ( action_source == import_images_menu_item ) {

			System.out.println ( "Opening new file ..." );
			// String old_path = current_base_path;

		  // MyFileChooser file_chooser = new MyFileChooser ( this.current_directory );
		  //FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files", "txt");
		  // FileNameExtensionFilter filter = new FileNameExtensionFilter("JPG & GIF Images", "jpg", "gif");
		  // file_chooser.setFileFilter(filter);
		  // data_set_chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		  file_chooser.setMultiSelectionEnabled(true);
		  int returnVal = file_chooser.showDialog(this, "Image Files to Add");
		  if ( returnVal == JFileChooser.APPROVE_OPTION ) {
		    File selected_files[] = file_chooser.getSelectedFiles();
		    if (selected_files.length > 0) {
		      for (int i=0; i<selected_files.length; i++) {
            System.out.println ( "You chose this file: " + selected_files[i] );
		      }
		    }
        System.out.println ( "You chose to open this file: " /* + chooser.getCurrentDirectory() + " / " */ + file_chooser.getSelectedFile() );
        String file_path_and_name = "?Unknown?";
        try {
          file_path_and_name = "" + file_chooser.getSelectedFile();
          File image_file = new File ( file_path_and_name );
          BufferedImage new_image;
          new_image = ImageIO.read(image_file);
          this.image_frame = new_image;

          if (this.image_frames == null) {
            this.image_frames = new BufferedImage[1];
          } else {
            BufferedImage temp[] = new BufferedImage[this.image_frames.length+1];
            for (int i=0; i<this.image_frames.length; i++) {
              temp[i] = this.image_frames[i];
            }
            this.image_frames = temp;
          }
          this.image_frames[this.image_frames.length-1] = this.image_frame;
          repaint();
        } catch (Exception oe) {
	        // this.image_frame = null;
	        JOptionPane.showMessageDialog(null, "File error for: " + file_path_and_name, "File Path Error", JOptionPane.WARNING_MESSAGE);
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
          ButtonGroup bg;

          JMenu program_menu = new JMenu("Program");

          JMenu series_menu = new JMenu("File");

            JMenu import_menu = new JMenu("Import");
              import_menu.add ( mi = zp.import_images_menu_item = new JMenuItem("Images...") );
              mi.addActionListener(zp);
            series_menu.add ( import_menu );

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
            bg = new ButtonGroup();
            System.out.println ( "Initializing Drawing Mode with drawing_mode: "  + zp.drawing_mode );
            mode_menu.add ( zp.move_menu_item = mi = new JRadioButtonMenuItem("Move", !zp.drawing_mode) );
            mi.addActionListener(zp);
            bg.add ( mi );
            mode_menu.add ( zp.draw_menu_item = mi = new JRadioButtonMenuItem("Draw", zp.drawing_mode) );
            mi.addActionListener(zp);
            bg.add ( mi );
            System.out.println ( "Initializing Center Drawing check box with: "  + zp.center_draw );
            mode_menu.add ( zp.center_draw_menu_item = mi = new JCheckBoxMenuItem("Center Drawing", zp.center_draw) );
            mi.addActionListener(zp);
            mode_menu.add ( mi = new JMenuItem("Dump") );
            mi.addActionListener(zp);
            mode_menu.add ( mi = new JMenuItem("Clear") );
            mi.addActionListener(zp);
            menu_bar.add ( mode_menu );

				f.setJMenuBar ( menu_bar );
				
				zp.setBackground ( new Color (0,0,0) );
		    zp.file_chooser = new MyFileChooser ( zp.current_directory );

        try {
			    zp.image_frame = ImageIO.read(new File("test.png"));
			    // int type = zp.image_frame.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : zp.image_frame.getType();
		    } catch (IOException e) {
			    System.out.println( "\n\nCannot open default \"test.png\" file:\n" + e + "\n" );
		    }

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
