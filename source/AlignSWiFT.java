/* This is a prototype of ArtAlign functionality. */
/* Add commands to the run_commands array below. */

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import java.io.*;
import java.awt.image.*;
import javax.imageio.ImageIO;

import org.w3c.dom.*;
import org.xml.sax.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
// Deprecated: import java.io.StringBufferInputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


class command_interface {
  static String[] run_commands = {
    "Align", "ArtAlign",
    "Align 1", "ArtAlign 1",
    "Align 1 2", "ArtAlign 1 2",
    "Align 1 2 3", "ArtAlign 1 2 3",
    "Align ...", "ArtAlign 1.2 2.3 3.4 4.5 5.6 6.7 7.8 8.9",
    "", ""
  };

  static int num_commands() {
    return (command_interface.run_commands.length/2);
  }

  static String get_name ( int i ) {
    return ( command_interface.run_commands[2*i] );
  }

  static String get_command ( int i ) {
    return ( command_interface.run_commands[(2*i)+1] );
  }

  static void run_command ( String command ) {
    Runtime rt = Runtime.getRuntime();
    try {
      // Execute the program in the command (assuming it's being run from this directory)
      System.out.println ( "" );
      System.out.println ( "=================================================================================" );
      System.out.println ( "Running " + System.getenv("PWD") + File.separator + command );
      System.out.println ( "=================================================================================" );

      // Run the command from this directory with any parameters
      rt.exec ( System.getenv("PWD") + File.separator + command );

      // Read and echo the output found in file ArtAlign.out
      File f = new File ( System.getenv("PWD") + File.separator + "ArtAlign.out" );
      BufferedReader br = new BufferedReader ( new FileReader ( f ) );
      boolean ended = false;
      do {
        try {
          String s = br.readLine();
          if (s == null) {
            ended = true;
          } else {
            System.out.println ( s );
          }
        } catch (IOException e) {
          ended = true;
        }
      } while (!ended);
      br.close();
      System.out.println ( "=================================================================================" );
    } catch ( Exception exec_except ) {
      System.out.println ( "Error running: " + exec_except );
    }
  }
  static void clear ( String header ) {
    Runtime rt = Runtime.getRuntime();
    try {
      // Clear (rewrite) the file ArtAlign.out
      File f = new File ( System.getenv("PWD") + File.separator + "ArtAlign.out" );
      BufferedWriter bw = new BufferedWriter ( new OutputStreamWriter ( new FileOutputStream ( f ) ) );
      bw.write ( header, 0, header.length() );
      bw.close();
    } catch ( Exception exec_except ) {
      System.out.println ( "Error clearing: " + exec_except );
    }
  }
}


class MyFileChooser extends JFileChooser {
  private static final long serialVersionUID = 1L;
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

class ArrowClass {
  int pts[][] = null;
  public void make_arrows ( int l, int w, int n ) {
    // **** N O T E: ****
    //   This constructor only works when w = 2 * l !!!
    //
    // System.out.println ( "Arctan(1,2) = " + (180*Math.atan2(1,2)/Math.PI) );
    // w is the width of the arrow at its base
    // l is the length of the arrow
    // n is the number of entries
    pts = new int[n][4];
    double arrow_angle = Math.atan2 ( w/2.0, l ); // rise, run
    // System.out.println ( "Arrow Angle = " + (arrow_angle * 180 / Math.PI) );
    double angle = 0;
    for (int i=0; i<n; i++) {
      angle = (2 * Math.PI * i) / n;
      if (true) {
        pts[i][0] = -(int) ( l * Math.cos(arrow_angle+angle) );
        pts[i][2] = -(int) ( l * Math.cos(arrow_angle-angle) );
        pts[i][1] = -(int) ( (w/2) * Math.sin(arrow_angle+angle) );
        pts[i][3] =  (int) ( (w/2) * Math.sin(arrow_angle-angle) );
      } else {
        pts[i][0] = -l;
        pts[i][2] = -l;
        pts[i][1] = -(w/2);
        pts[i][3] = (w/2);
      }
      // System.out.println ( "Angle=" + angle + " -> [ " + pts[i][0] + "," +  pts[i][1] + "," +  pts[i][2] + "," +  pts[i][3] + " ]" );
    }
  }

  public ArrowClass ( int l ) {
  make_arrows ( l, 2*l, 360 );
  }

  int[] get ( int dx, int dy ) {
    // Return the deltas appropriate for an arrow from the origin to dx,dy
    int index = ( pts.length + (int)( pts.length * ( Math.atan2 ( dy, dx ) / (2*Math.PI) ) ) ) % pts.length;
    return ( pts[index] );
  }
}


public class ArtAlign extends ZoomPanLib implements ActionListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
  private static final long serialVersionUID = 1L;

  JFrame parent_frame = null;
  static int w=1200, h=1000;

  boolean show_points = true;
  boolean show_arrows = false;
  boolean show_handles = true;
  boolean show_coords = false;

  boolean modify_mode = false;
  boolean editing_mode = false;

  boolean center_draw = false;
  boolean segment_draw = true;
  boolean bezier_draw = true;
  boolean bezier_locked = true;

  boolean export_handles = true;
  boolean stroke_started = false;

  String current_directory = "";
  MyFileChooser file_chooser = null;

  public ArrowClass arrows = new ArrowClass ( 12 );

  int line_padding = 1;
  int new_trace_color = 0xff0000;

  SeriesClass series = null;

  String current_trace_name = null;

  double[] click_point = null;

  double[] active_point = null;
  double[] active_h0 = null;
  double[] active_cp = null;
  double[] active_h1 = null;

  void dump_strokes() {
    if (series != null) {
      series.dump_strokes();
    }
  }

  void draw_scaled_line ( Graphics g, int xoffset, int yoffset, double x0, double y0, double x1, double y1 ) {
    g.drawLine ( xoffset+this.x_to_pxi(x0),   yoffset+this.y_to_pyi(y0),  xoffset+this.x_to_pxi(x1),  yoffset+this.y_to_pyi(y1) );
  }

  public void paint_frame (Graphics g) {
    Dimension win_s = getSize();
    int win_w = win_s.width;
    int win_h = win_s.height;
    if (recalculate) {
      set_scale_to_fit ( -10, 10, -10, 10, win_w, win_h );
      recalculate = false;
    }
    if (this.series != null) {
      try {
        this.series.paint_section(g, this);
      } catch (OutOfMemoryError mem_err) {
        System.out.println ( "ArtAlign.paint_frame: **** Out of Memory Error" );
      }
    }

    if (this.center_draw) {
      // Draw the white center cross on top of everything
      g.setColor ( new Color ( 255, 255, 255 ) );
      int cx = this.getSize().width / 2;
      int cy = this.getSize().height / 2;
      g.drawLine ( cx-10, cy, cx+10, cy );
      g.drawLine ( cx, cy-10, cx, cy+10 );
    }

  }


  public void set_cursor() {
    if (!modify_mode) {
      // This is move mode
      current_cursor = b_cursor;
      setCursor ( current_cursor );
    } else if (editing_mode) {
      current_cursor = Cursor.getPredefinedCursor ( Cursor.DEFAULT_CURSOR );
      setCursor ( current_cursor );
    } else {
      current_cursor = Cursor.getPredefinedCursor ( Cursor.CROSSHAIR_CURSOR );
      if (center_draw) {
        current_cursor = Cursor.getPredefinedCursor ( Cursor.HAND_CURSOR );
      }
      if (segment_draw) {
        current_cursor = Cursor.getPredefinedCursor ( Cursor.HAND_CURSOR );
      }
      if (bezier_draw) {
        current_cursor = Cursor.getPredefinedCursor ( Cursor.CROSSHAIR_CURSOR );
      }
      setCursor ( current_cursor );
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

    // Create the horizontal cursor
    p = new Polygon();
    p.addPoint ( 0, h/2 );
    p.addPoint ( w/4, (h/2)-(h/4) );
    p.addPoint ( w/4, (h/2)-(h/8) );
    p.addPoint ( 3*w/4, (h/2)-(h/8) );
    p.addPoint ( 3*w/4, (h/2)-(h/4) );
    p.addPoint ( w-1, h/2 );
    p.addPoint ( 3*w/4, (h/2)+(h/4) );
    p.addPoint ( 3*w/4, (h/2)+(h/8) );
    p.addPoint ( w/4, (h/2)+(h/8) );
    p.addPoint ( w/4, (h/2)+(h/4) );

    cursor_image = new BufferedImage(cursor_size,cursor_size,BufferedImage.TYPE_4BYTE_ABGR);
    cg = cursor_image.createGraphics();
    cg.setColor ( new Color(255,255,255) );
    cg.fillPolygon ( p );
    cg.setColor ( new Color(0,0,0) );
    cg.drawPolygon ( p );

    h_cursor = tk.createCustomCursor ( cursor_image, new Point(cursor_size/2,cursor_size/2), "Horizontal" );

    // Create the vertical cursor
    p = new Polygon();
    p.addPoint ( w/2, 0 );
    p.addPoint ( (w/2)+(w/4), h/4 );
    p.addPoint ( (w/2)+(w/8), h/4 );
    p.addPoint ( (w/2)+(w/8), 3*h/4 );
    p.addPoint ( (w/2)+(w/4), 3*h/4 );
    p.addPoint ( w/2, h-1 );
    p.addPoint ( (w/2)-(w/4), 3*h/4 );
    p.addPoint ( (w/2)-(w/8), 3*h/4 );
    p.addPoint ( (w/2)-(w/8), h/4 );
    p.addPoint ( (w/2)-(w/4), h/4 );

    cursor_image = new BufferedImage(cursor_size,cursor_size,BufferedImage.TYPE_4BYTE_ABGR);
    cg = cursor_image.createGraphics();
    cg.setColor ( new Color(255,255,255) );
    cg.fillPolygon ( p );
    cg.setColor ( new Color(0,0,0) );
    cg.drawPolygon ( p );

    v_cursor = tk.createCustomCursor ( cursor_image, new Point(cursor_size/2,cursor_size/2), "Vertical" );

    // Create the both cursor

    int cw2 = w/2;   // Cursor width / 2
    int ch2 = h/2;   // Cursor height / 2

    int clw = w/12;  // Arrow line width / 2
    int caw = w/6;   // Arrow head width / 2
    int cal = (int)(w/5);   // Arrow head length

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
    // current_cursor = b_cursor;
    // current_cursor = Cursor.getPredefinedCursor ( Cursor.MOVE_CURSOR );
    // current_cursor = Cursor.getPredefinedCursor ( Cursor.CROSSHAIR_CURSOR );
    this.set_cursor();
  }

  setCursor ( current_cursor );
  }

  public void mouseExited ( MouseEvent e ) {
    // System.out.println ( "Mouse exited" );
    super.mouseExited(e);
  }

  public void mouseClicked ( MouseEvent e ) {
    // System.out.println ( "Mouse clicked: " + e );
    if (e.getButton() == MouseEvent.BUTTON3) {
      // This is a mode change of some sort
      if (e.isShiftDown()) {
        // This is changing between drawing and editing
        editing_mode = !editing_mode;
      } else {
        // This is changing between moving and modifying
        modify_mode = !modify_mode;
      }
      // modify_mode = !modify_mode;
      set_cursor();
      repaint();
    } else {
      super.mouseClicked(e);
    }
  }

  public void mousePressed ( MouseEvent e ) {
    // System.out.println ( "Mouse pressed with modify_mode = " + modify_mode );
    super.mousePressed(e);
    if (editing_mode && modify_mode) {
      // System.out.println ( "Mouse pressed in edit mode at " + e.getX() + ", " + e.getY() );
      if (click_point == null) {
        click_point = new double[2];
      }
      click_point[0] = px_to_x(e.getX());
      click_point[1] = -py_to_y(e.getY());
      if (series != null) {
        double closest[] = series.find_closest ( click_point );
        double triplet[][] = series.find_bezier_triplet ( closest );
        if (closest == null) {
          // System.out.println ( "Series found no closest!" );
        } else {
          // System.out.println ( "Series found closest at " + closest[0] + ", " + closest[1] );
          // It would be good to have a limit radius here, but that can be done later
          double closest_px = x_to_pxi(closest[0]);
          double closest_py = y_to_pyi(closest[1]);
          double rect_dist = Math.min ( Math.abs(e.getX()-closest_px), Math.abs(e.getY()-closest_py) );
          if (rect_dist < 6) {
            active_point = closest;
            if (triplet != null) {
              active_h0 = triplet[0];
              active_cp = triplet[1];
              active_h1 = triplet[2];
            }
          } else {
            active_point = null;
            active_h0 = null;
            active_cp = null;
            active_h1 = null;
          }
          repaint();
        }
      }
    } else {
      active_point = null;
      active_h0 = null;
      active_cp = null;
      active_h1 = null;
      if (e.getButton() == MouseEvent.BUTTON1) {
        if (modify_mode == true) {
          if (segment_draw) {
          } else if (bezier_draw) {
          } else {
          }
          double p[] = { px_to_x(e.getX()), py_to_y(e.getY()) };
          if (center_draw) {
          p[0] = px_to_x(getSize().width / 2);
          p[1] = py_to_y(getSize().height / 2);
          }
          // System.out.println ( "Adding point " + p[0] + "," + p[1] );
          repaint();
        }
      }
    }
  }

  public void mouseReleased ( MouseEvent e ) {
    // System.out.println ( "Mouse released" );
    if (editing_mode && modify_mode) {
      // System.out.println ( "Mouse released in edit mode" );
    } else {
      if (modify_mode == false) {
        super.mouseReleased(e);
      } else {
        repaint();
      }
    }
  }


  public void rotate_about ( double center[], double master[], double slave[] ) {
    // Rotate the "slave" point about the "center" point to be aligned with (but opposite of) the "control" point
    // Modify the slave point in place
    double mx_rel = master[0] - center[0];
    double my_rel = master[1] - center[1];
    double sx_rel = slave[0] - center[0];
    double sy_rel = slave[1] - center[1];
    double dmaster = Math.sqrt ( (mx_rel * mx_rel) + (my_rel * my_rel) );
    double dslave = Math.sqrt ( (sx_rel * sx_rel) + (sy_rel * sy_rel) );
    if ( (dmaster > 0) && (dslave > 0) ) {
      double mx_unit = mx_rel / dmaster;
      double my_unit = my_rel / dmaster;
      slave[0] = center[0] - ( mx_unit * dslave );
      slave[1] = center[1] - ( my_unit * dslave );
    }
  }


  // MouseMotionListener methods:

  public void mouseDragged ( MouseEvent e ) {
    // System.out.println ( "Mouse dragged" );
    if (editing_mode && modify_mode) {
      // System.out.println ( "Mouse dragged in edit mode" );
      if (active_point != null) {
        double p[] = { px_to_x(e.getX()), -py_to_y(e.getY()) };
        double dx = p[0] - click_point[0];
        double dy = p[1] - click_point[1];
        if (active_point == active_cp) {
          // The center point was moved, so move all together
          active_point[0] += dx;
          active_point[1] += dy;
          if (active_h0 != null) {
            active_h0[0] += dx;
            active_h0[1] += dy;
          }
          if (active_h1 != null) {
            active_h1[0] += dx;
            active_h1[1] += dy;
          }
        } else if (active_point == active_h0) {
          // Handle 0 was moved
          if (active_h0 != null) {
            active_h0[0] += dx;
            active_h0[1] += dy;
            if (bezier_locked) {
              rotate_about ( active_cp, active_h0, active_h1 );
            }
          }
        } else if (active_point == active_h1) {
          // Handle 1 was moved
          if (active_h1 != null) {
            active_h1[0] += dx;
            active_h1[1] += dy;
            if (bezier_locked) {
              rotate_about ( active_cp, active_h1, active_h0 );
            }
          }
        }
        click_point = p;
        repaint();
      }
    } else {
      if (modify_mode == false) {
        super.mouseDragged(e);
      } else {
        if (segment_draw) {
          // Ignore mouse drags
        } else if (bezier_draw) {
          // Not sure what to do here
        } else {
          if (center_draw) {
            super.mouseDragged(e);
          }
          repaint();
        }
      }
    }
  }

  int cur_mouse_xi = 0;
  int cur_mouse_yi = 0;

  double cur_mouse_x = 0;
  double cur_mouse_y = 0;

  public void mouseMoved ( MouseEvent e ) {
    super.mouseMoved ( e );
    cur_mouse_xi = e.getX();
    cur_mouse_yi = e.getY();
    cur_mouse_x = px_to_x(cur_mouse_xi);
    cur_mouse_y = -py_to_y(cur_mouse_yi);
    if (this.show_coords) {
      System.out.println ( "  (" + cur_mouse_xi + "," + cur_mouse_yi + ") => (" + cur_mouse_x + "," + cur_mouse_y + ")" );
      repaint();
    }
  }

  // MouseWheelListener methods:
  public void mouseWheelMoved ( MouseWheelEvent e ) {
    /*
    if (e.isShiftDown()) System.out.println ( "Wheel Event with Shift" );
    if (e.isControlDown()) System.out.println ( "Wheel Event with Control" );
    if (e.isAltDown()) System.out.println ( "Wheel Event with Alt" );
    if (e.isMetaDown()) System.out.println ( "Wheel Event with Meta" );
    */
    if (this.series == null) {
      // Don't change the section index in this case
    } else {
      //if (modify_mode == true) {
      if (e.isShiftDown()) {
      // scroll_wheel_position += e.getWheelRotation();
      int scroll_wheel_delta = -e.getWheelRotation();
      int section_index = this.series.position_by_n_sections ( scroll_wheel_delta );
      if (this.parent_frame != null) {
        this.parent_frame.setTitle ( "Series: " + this.series.get_short_name() + ", Section: " + (section_index+1) );
        }
      } else {
        super.mouseWheelMoved ( e );
      }
    }
    repaint();
  }



  JMenuItem about_menu_item = null;
  JMenuItem menu_overview_menu_item = null;
  JMenuItem mouse_clicks_menu_item = null;

  JMenuItem version_menu_item = null;

  JMenuItem new_series_menu_item=null;
  JMenuItem open_series_menu_item=null;
  JMenuItem save_series_menu_item=null;
  JMenuItem import_images_menu_item=null;
  JMenuItem list_sections_menu_item=null;

  JMenuItem run_clear_menu_item=null;

  JMenuItem purge_images_menu_item = null;
  JMenuItem dump_menu_item=null;
  JMenuItem dump_areas_menu_item=null;
  JMenuItem clear_menu_item=null;
  JMenuItem fix_menu_item=null;

  JMenuItem exit_menu_item=null;

  // KeyListener methods:

  public void keyTyped ( KeyEvent e ) {
    // System.out.println ( "Key: " + e );
    if (Character.toUpperCase(e.getKeyChar()) == ' ') {
      // Space bar toggles between drawing mode and move mode
      if (!editing_mode) {
        modify_mode = !modify_mode;
        if (modify_mode) {
          current_cursor = Cursor.getPredefinedCursor ( Cursor.CROSSHAIR_CURSOR );
          if (center_draw) {
            current_cursor = Cursor.getPredefinedCursor ( Cursor.HAND_CURSOR );
          }
          setCursor ( current_cursor );
          stroke_started = false;
        } else {
          current_cursor = b_cursor;
          setCursor ( current_cursor );
          stroke_started = false;
        }
      }
    } else if (Character.toUpperCase(e.getKeyChar()) == 'P') {
    } else {
      // System.out.println ( "KeyEvent = " + e );
    }
    repaint();
  }

  public void keyPressed ( KeyEvent e ) {
    if ( (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) || (e.getKeyCode() == KeyEvent.VK_PAGE_UP  ) ) {
      // Figure out if there's anything to do
      if (this.series != null) {
        int delta = 0;
        if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
          //System.out.println ( "Page Up" );
          delta = 1;
        } else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
          //System.out.println ( "Page Down" );
          delta = -1;
        }
        if (delta != 0) {
          int section_index = this.series.position_by_n_sections ( delta );
            if (this.parent_frame != null) {
              this.parent_frame.setTitle ( "Series: " + this.series.get_short_name() + ", Section: " + (section_index+1) );
            }
          repaint();
        }
      }
    } else if ( e.getKeyCode() == KeyEvent.VK_HOME ) {
      this.recalculate = true;
      repaint();
    } else {
      // System.out.println ( "Key Pressed, e = " + e );
    }
    //super.keyPressed ( e );
    }
    public void keyReleased ( KeyEvent e ) {
    // System.out.println ( "Key Released" );
    //super.keyReleased ( e );
  }


  String new_series_file_name = null;

  // ActionPerformed methods (mostly menu responses):

  public void actionPerformed(ActionEvent e) {
    Object action_source = e.getSource();

    if ( action_source == version_menu_item ) {
      String s =
                "ArtAlign Java\n" +
                "\n" +
                "  Version 0.1\n" +
                "  July 31st, 2018\n" +
                "\n";
      JOptionPane.showMessageDialog(null, s, "ArtAlign Java Version", JOptionPane.INFORMATION_MESSAGE);
    } else if ( action_source == about_menu_item ) {
      String s =
                "ArtAlign is a prototype implementation supporting Art's alignment code.\n" +
                "\n" +
                "ArtAlign calls command line programs to perform the alignment.";
      JOptionPane.showMessageDialog(null, s, "About ArtAlign", JOptionPane.INFORMATION_MESSAGE);
    } else if ( action_source == menu_overview_menu_item ) {
      String s =
                "Most menu items are inoperable except the following:\n" +
                "\n" +
                "Program...\n" +
                "  Debug...\n" +
                "    Dump: Prints lots of internal data to the console.\n" +
                "    Dump Areas: Prints the areas for each contour on each section.\n" +
                "    Clear: Clears any traces currently being drawn.\n" +
                "  Exit: Exits the program.\n" +
                "\n" +
                "Series...\n" +
                "  New: Creates a new empty Series file (.ser).\n" +
                "  Open: Allows opening a Series file (.ser).\n" +
                "  Save: Saves the current Series with a user-selected name.\n" +
                "  Import/Images: Imports images to populate a new series file.\n" +
                "  Purge Images: Remove images from memory (helps with memory constraints).\n" +
                "\n";
      JOptionPane.showMessageDialog(null, s, "ArtAlign Java Menu Overview", JOptionPane.INFORMATION_MESSAGE);
    } else if ( action_source == mouse_clicks_menu_item ) {
      String s =
                "Mouse Operation:\n" +
                "\n" +
                "  Move Mode (4-arrow cursor):\n" +
                "    Scroll Wheel zooms (scales the view).\n" +
                "    Shifted Scroll Wheel moves through sections.\n" +
                "    Left Click and Drag moves (pans) the view.\n" +
                "    Right Click switches to \"Modify Mode\" (Draw or Edit).\n" +
                "\n";
      JOptionPane.showMessageDialog(null, s, "ArtAlign Java Mouse Operation", JOptionPane.INFORMATION_MESSAGE);
    } else if ( action_source == new_series_menu_item ) {
      file_chooser.setMultiSelectionEnabled(false);
      FileNameExtensionFilter filter = new FileNameExtensionFilter("Series Files", "ser");
      file_chooser.setFileFilter(filter);
      if (file_chooser.getName() == null) {
        // Try to set the default file name of newSeries.ser
      }
      int returnVal = file_chooser.showDialog(this, "New Series");
      if ( returnVal == JFileChooser.APPROVE_OPTION ) {
        File series_file = file_chooser.getSelectedFile();
        System.out.println ( "You chose to create this file: " /* + chooser.getCurrentDirectory() + " / " */ + series_file );
        try {
          DataOutputStream f = new DataOutputStream ( new FileOutputStream ( series_file ) );
          f.writeBytes ( ArtAlignDefaults.convert_newlines ( ArtAlignDefaults.default_series_file_string ) );
          f.close();
          this.new_series_file_name = series_file.getPath();
          this.series = new SeriesClass();
        } catch (Exception oe) {
          System.out.println ( "Error while writing a series file:\n" + oe );
          oe.printStackTrace();
          JOptionPane.showMessageDialog(null, "File write error", "File Write Error", JOptionPane.WARNING_MESSAGE);
        }
        repaint();
      }
    } else if ( action_source == open_series_menu_item ) {
      file_chooser.setMultiSelectionEnabled(false);
      FileNameExtensionFilter filter = new FileNameExtensionFilter("Series Files", "ser");
      file_chooser.setFileFilter(filter);
      int returnVal = file_chooser.showDialog(this, "Open Series");
      if ( returnVal == JFileChooser.APPROVE_OPTION ) {
        File series_file = file_chooser.getSelectedFile();
        System.out.println ( "You chose to open this file: " /* + chooser.getCurrentDirectory() + " / " */ + series_file );

        try {
          this.series = new SeriesClass ( series_file );
        } catch (Exception oe) {
          // this.series.image_frame = null;
          System.out.println ( "Error while opening a series file:\n" + oe );
          oe.printStackTrace();
          JOptionPane.showMessageDialog(null, "File error", "File Path Error", JOptionPane.WARNING_MESSAGE);
        }
        repaint();
      }
    } else if ( action_source == save_series_menu_item ) {
      file_chooser.setMultiSelectionEnabled(false);
      FileNameExtensionFilter filter = new FileNameExtensionFilter("Series Files", "ser");
      file_chooser.setFileFilter(filter);
      int returnVal = file_chooser.showDialog(this, "Save Series");
      if ( returnVal == JFileChooser.APPROVE_OPTION ) {
        File series_file = file_chooser.getSelectedFile();
        System.out.println ( "You chose to save to this file: " /* + chooser.getCurrentDirectory() + " / " */ + series_file );
        try {
          this.series.write_as_xml ( series_file, this );
        } catch (Exception oe) {
          // this.series.image_frame = null;
          System.out.println ( "Error while saving a series file:\n" + oe );
          oe.printStackTrace();
          JOptionPane.showMessageDialog(null, "File error", "File Path Error", JOptionPane.WARNING_MESSAGE);
        }
        repaint();
      }
    } else if ( action_source == dump_menu_item ) {
      if (this.series != null) {
        System.out.println ( ">>>>>>>>>>>>>>>>>>> Action: dump_menu_item" );
        System.out.println ( ">>>>>>>>>>>>>>>>>>> dump_xml" );
        this.series.dump_xml();
        System.out.println ( ">>>>>>>>>>>>>>>>>>> dump_strokes" );
        this.series.dump_strokes();
      }
    } else if ( action_source == dump_areas_menu_item ) {
      if (this.series != null) {
        System.out.println ( ">>>>>>>>>>>>>>>>>>> Action: dump_areas_menu_item" );
        this.series.dump_areas();
      }
    } else if ( action_source == purge_images_menu_item ) {
      if (this.series != null) {
        this.series.purge_images();
      }
      repaint();
    } else if ( action_source == clear_menu_item ) {
      if (this.series != null) {
        this.series.clear_strokes();
      }
      repaint();
    } else if ( action_source == fix_menu_item ) {
      if (this.series != null) {
        this.series.fix_handles();
      }
      repaint();
    } else if ( action_source == import_images_menu_item ) {
      file_chooser.setMultiSelectionEnabled(true);
      FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", "jpg", "gif", "png", "tiff");
      file_chooser.setFileFilter(filter);
      int returnVal = file_chooser.showDialog(this, "Import Images");
      if ( returnVal == JFileChooser.APPROVE_OPTION ) {
        File image_files[] = file_chooser.getSelectedFiles();
        // Should have sorting options to properly order names like: xyz8.jpg xyz9.jpg xyz10.jpg xyz11.jpg
        System.out.println ( "You chose to import these " + image_files.length + " images:" );
        for (int i=0; i<image_files.length; i++) {
          System.out.println ( "  " + image_files[i] );
        }
        // ArtAlign.exe creates the section files as soon as the images have been imported so do it now
        this.series = new SeriesClass ( this.new_series_file_name );
        this.series.import_images ( image_files );
      }
    } else if ( action_source == list_sections_menu_item ) {
      System.out.println ( "Sections: ..." );
    } else if ( action_source == exit_menu_item ) {
      System.exit ( 0 );
    } else if ( action_source == run_clear_menu_item ) {
      command_interface.clear("" + System.currentTimeMillis() + "\n");
    } else {
      String cmd = e.getActionCommand();
      boolean found = false;
      for (int i=0; i<command_interface.num_commands(); i++) {
        if (cmd == command_interface.get_name(i)) {
          found = true;
          String actual_command = command_interface.get_command(i);
          // System.out.println ( "Found " + cmd + " executes " + actual_command );
          if (actual_command.length() > 0) {
            command_interface.run_command ( actual_command );
          }
          break;
        }
      }
      if (!found) {
        JOptionPane.showMessageDialog(null, "Option Not Implemented", "Option Not Implemented", JOptionPane.WARNING_MESSAGE);
      }
    }
    if ( (this.parent_frame != null) && (this.series != null) ) {
      this.parent_frame.setTitle ( "Series: " + this.series.get_short_name() + ", Section: " + (this.series.get_position()+1) );
    }

  }


  public static void main ( String[] args ) {

    /* Properties returned by: System.getProperties();
    "awt.toolkit"
    "java.specification.version"
    "file.encoding.pkg"
    "sun.cpu.isalist"
    "sun.jnu.encoding"
    "java.class.path"
    "java.vm.vendor"
    "sun.arch.data.model"
    "java.vendor.url"
    "user.timezone"
    "os.name"
    "java.vm.specification.version"
    "sun.java.launcher"
    "user.country"
    "sun.boot.library.path"
    "sun.java.command"
    "jdk.debug"
    "sun.cpu.endian"
    "user.home"
    "user.language"
    "java.specification.vendor"
    "java.home"
    "file.separator"
    "java.vm.compressedOopsMode"
    "line.separator"
    "java.vm.specification.vendor"
    "java.specification.name"
    "java.awt.graphicsenv"
    "sun.management.compiler"
    "java.runtime.version"
    "user.name"
    "path.separator"
    "os.version"
    "java.runtime.name"
    "file.encoding"
    "java.vm.name"
    "java.vendor.url.bug"
    "java.io.tmpdir"
    "java.version"
    "user.dir"
    "os.arch"
    "java.vm.specification.name"
    "java.awt.printerjob"
    "sun.os.patch.level"
    "java.library.path"
    "java.vm.info"
    "java.vendor"
    "java.vm.version"
    "sun.io.unicode.encoding"
    "java.class.version"

    */

    // Set the current_newline_string as appropriate for each operating system
    String os_name = System.getProperty ( "os.name", "Unknown" );
    if ( os_name.startsWith("W") || os_name.startsWith("w") ) {
      ArtAlignDefaults.current_newline_string = "\r\n";  // CR+LF
    } else {
      ArtAlignDefaults.current_newline_string = "\n";    // LF
    }

    System.out.println ( "Operating System is " + os_name );
    for (int i=0; i<args.length; i++) {
      System.out.println ( "Arg[" + i + "] = \"" + args[i] + "\"" );
    }

    // System.out.println ( "ArtAlign: Use the mouse wheel to zoom, and drag to pan." );
    javax.swing.SwingUtilities.invokeLater ( new Runnable() {
      public void run() {
        JFrame f = new JFrame("./ArtAlign - No Active Series");
        f.setDefaultCloseOperation ( JFrame.EXIT_ON_CLOSE );

        ArtAlign zp = new ArtAlign();
        ArtAlignDefaults.current_newline_string = "\n";  // For Windows this may be set to: "\r\n"

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
          JMenu windows_menu = new JMenu("Windows");
            windows_menu.add ( mi = new JCheckBoxMenuItem("Tools window", true) );
            mi.addActionListener(zp);
            windows_menu.add ( mi = new JCheckBoxMenuItem("Status bar", true) );
            mi.addActionListener(zp);
          program_menu.add ( windows_menu );

        program_menu.addSeparator();

        JMenu debug_menu = new JMenu("Debug");
          debug_menu.add ( mi = new JCheckBoxMenuItem("Logging", true) );
          mi.addActionListener(zp);
          debug_menu.add ( mi = new JMenuItem("Times") );
          mi.addActionListener(zp);
          debug_menu.addSeparator();
          debug_menu.add ( zp.dump_menu_item = mi = new JMenuItem("Dump") );
          mi.addActionListener(zp);
          debug_menu.add ( zp.dump_areas_menu_item = mi = new JMenuItem("Dump Areas") );
          mi.addActionListener(zp);
          debug_menu.add ( zp.clear_menu_item = mi = new JMenuItem("Clear") );
          mi.addActionListener(zp);
          debug_menu.add ( zp.fix_menu_item = mi = new JMenuItem("Fix Handles") );
          mi.addActionListener(zp);
        program_menu.add ( debug_menu );

        program_menu.add ( zp.exit_menu_item = mi = new JMenuItem("Exit") );
        mi.addActionListener(zp);
        menu_bar.add ( program_menu );


        JMenu series_menu = new JMenu("Series");

        series_menu.add ( zp.new_series_menu_item = mi = new JMenuItem("New...") );
        mi.addActionListener(zp);

        series_menu.addSeparator();

        series_menu.add ( zp.open_series_menu_item = mi = new JMenuItem("Open...") );
        mi.addActionListener(zp);

        series_menu.add ( zp.save_series_menu_item = mi = new JMenuItem("Save") );
        mi.addActionListener(zp);

        series_menu.addSeparator();

        series_menu.add ( zp.import_images_menu_item = mi = new JMenuItem("Import Images...") );
        mi.addActionListener(zp);

        series_menu.addSeparator();

        series_menu.add ( zp.purge_images_menu_item = mi = new JMenuItem("Purge Images") );
        mi.addActionListener(zp);

        menu_bar.add ( series_menu );


        JMenu run_menu = new JMenu("Run");

        series_menu.addSeparator();
        for (int cmd_num=0; cmd_num<command_interface.num_commands(); cmd_num++) {
          JMenuItem temp_menu_item = new JMenuItem(command_interface.get_name(cmd_num));
          run_menu.add ( temp_menu_item );
          temp_menu_item.addActionListener(zp);
        }

        series_menu.addSeparator();
        run_menu.add ( zp.run_clear_menu_item = mi = new JMenuItem("Clear Run Log") );
        mi.addActionListener(zp);


        menu_bar.add ( run_menu );


        JMenu help_menu = new JMenu("Help");
        help_menu.add ( zp.about_menu_item = mi = new JMenuItem("About...") );
        mi.addActionListener(zp);
        help_menu.addSeparator();
        help_menu.add ( zp.menu_overview_menu_item = mi = new JMenuItem("Menu Items...") );
        mi.addActionListener(zp);
        help_menu.add ( mi = new JMenuItem("Key commands...") );
        mi.addActionListener(zp);
        help_menu.add ( zp.mouse_clicks_menu_item = mi = new JMenuItem("Mouse clicks...") );
        mi.addActionListener(zp);
        help_menu.addSeparator();
        help_menu.add ( zp.version_menu_item = mi = new JMenuItem("Version...") );
        mi.addActionListener(zp);
        menu_bar.add ( help_menu );

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

