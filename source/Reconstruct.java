/* This is a prototype of Reconstruct functionality. */

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

  /*
  public ArrowClass ( int l, int n ) {
  make_arrows ( l, 2*l, n );
  }
  */

  public ArrowClass ( int l ) {
  make_arrows ( l, 2*l, 360 );
  }

  int[] get ( int dx, int dy ) {
    // Return the deltas appropriate for an arrow from the origin to dx,dy
    int index = ( pts.length + (int)( pts.length * ( Math.atan2 ( dy, dx ) / (2*Math.PI) ) ) ) % pts.length;
    return ( pts[index] );
  }
}


public class Reconstruct extends ZoomPanLib implements ActionListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
  private static final long serialVersionUID = 1L;

  JFrame parent_frame = null;
  static int w=1200, h=1000;

  int selection_radius = 5;

  boolean show_points = true;
  boolean show_arrows = false;
  boolean show_handles = true;
  boolean show_coords = false;
  boolean show_glitch = false;

  boolean modify_mode = false;
  boolean editing_mode = false;
  boolean insert_mode = false;
  boolean delete_mode = false;

  boolean center_draw = false;
  boolean segment_draw = true;
  boolean bezier_draw = false;
  boolean bezier_locked = true;

  boolean export_handles = true;
  boolean stroke_started = false;

  String current_directory = "";
  MyFileChooser file_chooser = null;

  public ArrowClass arrows = new ArrowClass ( 12 );

  int line_padding = 1;
  int new_trace_color = 0xff0000;
  double glitch_angle_degrees = 45.0;

  SeriesClass series = null;
  ContourClass active_contour = null;

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
        System.out.println ( "Reconstruct.paint_frame: **** Out of Memory Error" );
      }
    }

    if (active_contour != null) {
      active_contour.draw ( g, this );
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
    if ( (modify_mode && editing_mode && delete_mode) ) {
      setCursor ( x_cursor );
    } else if ( (modify_mode && editing_mode && insert_mode) ) {
      setCursor ( a_cursor );
    } else if (!modify_mode) {
      // This is move mode
      current_cursor = b_cursor;
      setCursor ( current_cursor );
      //if (move_menu_item != null) {
      //  move_menu_item.setSelected(true);
      //}
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
      //if (draw_menu_item != null) {
      //  draw_menu_item.setSelected(true);
      //}
    }
  }


  //  MouseListener methods:


  Cursor current_cursor = null;
  Cursor h_cursor = null;
  Cursor v_cursor = null;
  Cursor b_cursor = null;
  Cursor x_cursor = null;
  Cursor a_cursor = null;
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

    // Create the x cursor

    p = new Polygon();
    p.addPoint ( 0, 2 );  // Top of center crossing
    p.addPoint ( cw2-2, ch2 );
    p.addPoint ( cw2, ch2-2 );

    p.addPoint ( 2, 0 );  // Right of center crossing
    p.addPoint ( cw2, -(ch2-2) );
    p.addPoint ( cw2-2, -ch2 );

    p.addPoint ( 0, -2 );  // Bottom of center crossing
    p.addPoint ( -(cw2-2), -ch2 );
    p.addPoint ( -cw2, -(ch2-2) );

    p.addPoint ( -2, 0 );  // Left of center crossing
    p.addPoint ( -cw2, ch2-2 );
    p.addPoint ( -(cw2-2), ch2 );

    p.translate ( w/2, h/2 );

    cursor_image = new BufferedImage(cursor_size,cursor_size,BufferedImage.TYPE_4BYTE_ABGR);
    cg = cursor_image.createGraphics();
    cg.setColor ( new Color(255,255,255) );
    cg.fillPolygon ( p );
    cg.setColor ( new Color(0,0,0) );
    cg.drawPolygon ( p );

    x_cursor = tk.createCustomCursor ( cursor_image, new Point(cursor_size/2,cursor_size/2), "X" );

    // Create the + cursor

    p = new Polygon();
    p.addPoint ( 1,1 );  // Upper/Right in center crossing
    p.addPoint ( cw2, 1 );
    p.addPoint ( cw2, -1 );

    p.addPoint (  1, -1 );  // Lower/Right of center crossing
    p.addPoint (  1, -ch2 );
    p.addPoint ( -1, -ch2 );

    p.addPoint ( -1, -1 );  // Lower/Left of center crossing
    p.addPoint ( -cw2, -1 );
    p.addPoint ( -cw2, 1 );

    p.addPoint ( -1, 1 );  // Upper/left of center crossing
    p.addPoint ( -1, ch2 );
    p.addPoint (  1, ch2 );

    p.translate ( w/2, h/2 );

    cursor_image = new BufferedImage(cursor_size,cursor_size,BufferedImage.TYPE_4BYTE_ABGR);
    cg = cursor_image.createGraphics();
    cg.setColor ( new Color(255,255,255) );
    cg.fillPolygon ( p );
    cg.setColor ( new Color(255,255,255) );
    cg.drawPolygon ( p );

    a_cursor = tk.createCustomCursor ( cursor_image, new Point(cursor_size/2,cursor_size/2), "+" );

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
      // System.out.println ( "Right Mouse: " + e.getSource() + " " + e.getID() + " "  + e.getWhen() + " "  + e.getModifiers() + " "  + e.getX() + " "  + e.getY() + " "  + e.getClickCount() + " "  + e.isPopupTrigger() + " "  + e.getButton() );
      // This is a mode change of some sort
      if (e.isShiftDown()) {
        // This is changing between drawing and editing
        editing_mode = !editing_mode;
      } else {
        // This is changing between moving and modifying
        modify_mode = !modify_mode;
      }
      if (segment_draw || bezier_draw) {
        if (active_contour != null) {
          active_contour.close();
          active_contour.init_bezier ( active_contour.is_bezier );  // Recompute beziers after closing
          active_contour.fix_handles();
          if (series != null) {
            series.add_contour ( active_contour );
          }
        }
        active_contour = null;
        // segment_draw = false;
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
    if (e.getButton() != MouseEvent.BUTTON3) {
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
          /*
          if (triplet == null) {
            System.out.println ( "Bezier Triplet is null" );
          } else {
            System.out.println ( "Bezier Triplet is not null" );
          }
          */
          if (closest == null) {
            // System.out.println ( "Series found no closest!" );
          } else {
            // System.out.println ( "Series found closest at " + closest[0] + ", " + closest[1] );
            // It would be good to have a limit radius here, but that can be done later
            double closest_px = x_to_pxi(closest[0]);
            double closest_py = y_to_pyi(closest[1]);
            double rect_dist = Math.min ( Math.abs(e.getX()-closest_px), Math.abs(e.getY()-closest_py) );
            if (delete_mode) {
              if (rect_dist < selection_radius) {
                System.out.println ( "Deleting point at mouse " + closest[0] + "," + closest[1] );
                series.delete_point ( closest );
                set_cursor();
              }
            } else if (insert_mode) {
              double p[] = { px_to_x(e.getX()), -py_to_y(e.getY()) };
              System.out.println ( "Inserting point at mouse " + p[0] + "," + p[1] );
              series.insert_point ( p );
              set_cursor();
            } else {
              if (rect_dist < selection_radius) {
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
              if (active_contour != null) {
                // System.out.println ( "Saving previous stroke" );
                if (series != null) {
                  active_contour.close();
                  active_contour.init_bezier ( active_contour.is_bezier );  // Recompute beziers after closing
                  active_contour.fix_handles();
                  series.add_contour ( active_contour );
                }
              }
            }
            if (active_contour == null) {
              // System.out.println ( "Making new stroke" );
              active_contour = new ContourClass ( new ArrayList<double[]>(100), new_trace_color, false, bezier_draw );
              active_contour.contour_name = current_trace_name;
              active_contour.is_bezier = bezier_draw;
            }
            double p[] = { px_to_x(e.getX()), py_to_y(e.getY()) };
            if (center_draw) {
              p[0] = px_to_x(getSize().width / 2);
              p[1] = py_to_y(getSize().height / 2);
            }
            // System.out.println ( "Adding point " + p[0] + "," + p[1] );
            double contour_point[] = { p[0], -p[1] };
            active_contour.add_point ( contour_point );
            active_contour.init_bezier ( active_contour.is_bezier );
            repaint();
          }
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
        if (active_contour != null) {
          double p[] = { px_to_x(e.getX()), py_to_y(e.getY()) };
          if (segment_draw) {
            // ?? active_contour.add_point ( p );
          } else if (bezier_draw) {
          } else {
            if (center_draw) {
              p[0] = px_to_x(getSize().width / 2);
              p[1] = py_to_y(getSize().height / 2);
            }
            double contour_point[] = { p[0], -p[1] };
            active_contour.add_point ( contour_point );
            active_contour.init_bezier ( active_contour.is_bezier );
            if (series != null) {
              active_contour.close();
              active_contour.init_bezier ( active_contour.is_bezier );  // Recompute beziers after closing
              active_contour.fix_handles();
              series.add_contour ( active_contour );
            }
            active_contour = null;
          }
          repaint();
        }
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
          if (active_contour == null) {
            active_contour  = new ContourClass ( new ArrayList<double[]>(100), new_trace_color, false, bezier_draw );
            active_contour.is_bezier = bezier_draw;
          }
          if (active_contour != null) {
            double p[] = { px_to_x(e.getX()), py_to_y(e.getY()) };
            if (center_draw) {
              p[0] = px_to_x(getSize().width / 2);
              p[1] = py_to_y(getSize().height / 2);
            }
            double contour_point[] = { p[0], -p[1] };
            active_contour.add_point ( contour_point );
            active_contour.init_bezier ( active_contour.is_bezier );
            repaint();
          }
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
        if (super.scroll_wheel_position < -75) {
          super.scroll_wheel_position = -75;
        }
        if (super.scroll_wheel_position > 75) {
          super.scroll_wheel_position = 75;
        }
        super.mouseWheelMoved ( e );
        System.out.println ( "Scale = " + super.scale );
        System.out.println ( "Wheel = " + super.scroll_wheel_position );
      }
    }
    repaint();
  }



  JMenuItem draw_only_menu_item = null;
  JMenuItem about_menu_item = null;
  JMenuItem menu_overview_menu_item = null;
  JMenuItem mouse_clicks_menu_item = null;
  JMenuItem hot_keys_menu_item = null;

  JMenuItem version_menu_item = null;

  JMenuItem move_menu_item = null;
  JMenuItem draw_menu_item = null;
  JMenuItem edit_menu_item = null;
  JMenuItem center_draw_menu_item = null;
  JCheckBoxMenuItem segment_draw_menu_item = null;
  JCheckBoxMenuItem bezier_draw_menu_item = null;
  JCheckBoxMenuItem bezier_locked_menu_item = null;

  JRadioButtonMenuItem sel_rad_1 = null;
  JRadioButtonMenuItem sel_rad_5 = null;
  JRadioButtonMenuItem sel_rad_10 = null;
  JRadioButtonMenuItem sel_rad_15 = null;

  JMenuItem new_series_menu_item=null;
  JMenuItem open_series_menu_item=null;
  JMenuItem save_series_menu_item=null;
  JMenuItem import_images_menu_item=null;
  JMenuItem list_sections_menu_item=null;


  JMenuItem crlf_series_menu_item=null;
  JMenuItem lf_series_menu_item=null;

  JMenuItem reverse_all_traces_menu_item=null;

  JMenuItem series_options_menu_item=null;

  JMenuItem line_menu_none_item = null;
  JMenuItem line_menu_0_item = null;
  JMenuItem line_menu_1_item = null;
  JMenuItem line_menu_2_item = null;
  JMenuItem show_points_menu_item = null;
  JMenuItem show_arrows_menu_item = null;
  JMenuItem show_handles_menu_item = null;
  JMenuItem show_coords_menu_item = null;
  JMenuItem show_glitch_menu_item = null;
  JMenuItem glitch_options_menu_item=null;

  JMenuItem export_handles_menu_item = null;

  JMenuItem color_menu_red_item = null;
  JMenuItem color_menu_green_item = null;
  JMenuItem color_menu_blue_item = null;
  JMenuItem color_menu_yellow_item = null;
  JMenuItem color_menu_magenta_item = null;
  JMenuItem color_menu_cyan_item = null;
  JMenuItem color_menu_black_item = null;
  JMenuItem color_menu_white_item = null;

  JMenuItem color_menu_half_item = null;

  JMenuItem color_menu_light_item = null;
  JMenuItem color_menu_dark_item = null;


  JMenuItem purge_images_menu_item = null;
  JMenuItem dump_menu_item=null;
  JMenuItem dump_areas_menu_item=null;
  JMenuItem clear_menu_item=null;
  JMenuItem fix_menu_item=null;
  JMenuItem add_section_menu_item=null;


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
          if (draw_menu_item != null) {
            draw_menu_item.setSelected(true);
          }
        } else {
          current_cursor = b_cursor;
          setCursor ( current_cursor );
          stroke_started = false;
          if (move_menu_item != null) {
            move_menu_item.setSelected(true);
          }
        }
      }
    } else if (Character.toUpperCase(e.getKeyChar()) == 'P') {
    } else if ( (Character.toUpperCase(e.getKeyChar()) == 'A') || (Character.toUpperCase(e.getKeyChar()) == 'I') ) {
      System.out.println ( "Add a point between two nearest points" );
      //System.out.println ( "Insert at the nearest point" );
      //System.out.println ( "KeyEvent = " + e );
      if (modify_mode && editing_mode) {
        delete_mode = false;
        insert_mode = !insert_mode;
        set_cursor();
        System.out.println ( "Click to insert a point" );
      }
    } else if ( (Character.toUpperCase(e.getKeyChar()) == 'X') || (Character.toUpperCase(e.getKeyChar()) == 'D') ) {
      //System.out.println ( "Delete the nearest point" );
      //System.out.println ( "KeyEvent = " + e );
      if (modify_mode && editing_mode) {
        insert_mode = false;
        delete_mode = !delete_mode;
        set_cursor();
        System.out.println ( "Click to delete a single point" );
      }
    } else if (Character.toUpperCase(e.getKeyChar()) == 'N') {
      System.out.println ( "Begin drawing a new trace" );
      // New trace mode is modify_mode but not editing_mode
      if (editing_mode) {
        // Send the right mouse click with shift enabled (5) to leave editing mode
        this.mouseClicked ( new MouseEvent(this, 500, 0, 5, 100, 100, 1, false, 3) ); // Right mouse with shift enabled
      }
      if (!modify_mode) {
        // Send the right mouse click without shift enabled (4) to enter modify mode
        this.mouseClicked ( new MouseEvent(this, 500, 0, 4, 100, 100, 1, false, 3) ); // Right mouse without shift enabled
      }
    } else if (Character.toUpperCase(e.getKeyChar()) == 'E') {
      System.out.println ( "Begin editing existing traces" );
      // Call the mouseClicked function with a faked MouseEvent
      /* public MouseEvent(
            Component source, // use this
            int id,           // use 300
            long when,        // use 0
            int modifiers,    // use 4 (unshifted) or use 5 (shifted)
            int x,
            int y,
            int clickCount,
            boolean popupTrigger,
            int button)
      */
      // Editing mode is both editing_mode and modify_mode set to true
      if ( (editing_mode) && (modify_mode) ) {
        // Already editing, so switch to move mode (more natural option)
        // Send the right mouse click without shift enabled (4) to leave modify mode
        this.mouseClicked ( new MouseEvent(this, 500, 0, 4, 100, 100, 1, false, 3) ); // Right mouse without shift enabled
      } else {
        // Not in editing mode, so enter as needed
        // Set them both to false, and send mouse clicks to enable them both
        if (!editing_mode) {
          // Send the right mouse click with shift enabled (5) to enter editing mode
          this.mouseClicked ( new MouseEvent(this, 500, 0, 5, 100, 100, 1, false, 3) ); // Right mouse with shift enabled
        }
        if (!modify_mode) {
          // Send the right mouse click without shift enabled (4) to enter modify mode
          this.mouseClicked ( new MouseEvent(this, 500, 0, 4, 100, 100, 1, false, 3) ); // Right mouse without shift enabled
        }
      }
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

  AlignSWiFT swift = new AlignSWiFT();

  // ActionPerformed methods (mostly menu responses):

  public void actionPerformed(ActionEvent e) {
    Object action_source = e.getSource();

    String cmd = e.getActionCommand();
    // System.out.println ( "ActionPerformed got \"" + cmd + "\"" );
    // System.out.println ( "ActionPerformed got \"" + action_source + "\"" );

    if ( action_source == version_menu_item ) {
      String s =
                "Reconstruct Java\n" +
                "\n" +
                "  Version 0.61\n" +
                "  August 8th, 2018\n" +
                "\n";
      JOptionPane.showMessageDialog(null, s, "Reconstruct Java Version", JOptionPane.INFORMATION_MESSAGE);
    } else if ( action_source == about_menu_item ) {
      String s =
                "Reconstruct Java is a prototype implementation of Reconstruct written in Java.\n" +
                "\n" +
                "Reconstruct Java was written to prove the feasability of using Java for this task. \n" +
                "\n" +
                "Reconstruct Java provides some useful functionality, but most of its features are \n" +
                "not fully implemented in this version.";
      JOptionPane.showMessageDialog(null, s, "About Reconstruct Java", JOptionPane.INFORMATION_MESSAGE);
    } else if ( action_source == draw_only_menu_item ) {
      String s =
                "To create a project with 5 empty sections:\n" +
                "\n" +
                "  Series / New  (give it a new name ending with \".ser\")\n" +
                "  Program / Debug / New Empty Sections\n" +
                "\n" +
                "It will complain that it cannot open the file named:\n" +
                "\n" +
                "    \"File_that_does_not_exist.jpg\"\n" +
                "\n" +
                "Click \"OK\" to dismiss the dialog box.\n" +
                "\n" +
                "The project will now have 5 empty sections with no images.\n" +
                "This can be useful for testing various drawing capabilities.\n" +
                "\n" +
                "Note that for adding and deleting points, Bezier drawing must be off:\n" +
                "\n" +
                "  Extras / Mode / Bezier Drawing";
      JOptionPane.showMessageDialog(null, s, "Create a drawing only project", JOptionPane.INFORMATION_MESSAGE);
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
                "  Open: Allows opening a Series file (.ser).\n" +
                "  New: Creates a new empty Series file (.ser).\n" +
                "  Save: Saves the current Series with a user-selected name.\n" +
                "  Options: Allows setting the current trace (object) name.\n" +
                "  Convert to CRLF: Converts any selected series to CRLF line endings.\n" +
                "  Convert to LF: Converts any selected series to LF line endings.\n" +
                "  Import/Images: Imports images to populate a new series file.\n" +
                "\n" +
                "Trace...\n" +
                "  Reverse All: Reverses all traces (doesn't work for Bezier traces).\n" +
                "\n" +
                "Extras...\n" +
                "  Color/<color name>: Chooses a color for all subsequent traces.\n" +
                "  Color/Half: Sets color to 50% of full value for subsequent traces.\n" +
                "  Color/Lighter: Brightens the current color for subsequent traces.\n" +
                "  Color/Darker: Darkens the current color for subsequent traces.\n" +
                "\n" +
                "  Line/None: Sets line width to zero (lines aren't drawn).\n" +
                "  Line/Width=#: Sets the line width to that number of pixels.\n" +
                "  Line/Show Points: Draws an indicator for each point (and handle).\n" +
                "  Line/Show Arrows: Draws start box and arrows for each trace segment.\n" +
                "  Line/Show Handles: Draws handles on Bezier curve points.\n" +
                "\n" +
                // "  Mode/Move: Click and drag of left mouse button will shift the display.\n" +
                // "  Mode/Draw: Click and drag of left mouse button will draw traces.\n" +
                "  Mode/Segment Drawing: Traces are drawn click by click (not by dragging).\n" +
                "  Mode/Bezier Drawing: Uses Bezier Curves when Segment Drawing is enabled.\n" +
                // "  Mode/Center Drawing: Image is dragged under a \"pen\" at the center.\n" +
                "\n" +
                "  Purge Images: Remove images from memory (helps with memory constraints).\n" +
                "\n" +
                "Any inoperable menu items should report \"Option Not Implemented\".\n" +
                "\n";
      JOptionPane.showMessageDialog(null, s, "Reconstruct Java Menu Overview", JOptionPane.INFORMATION_MESSAGE);
    } else if ( action_source == mouse_clicks_menu_item ) {
      String s =
                "Mouse Operation:\n" +
                "\n" +
                "  Move Mode (4-arrow cursor):\n" +
                "    Scroll Wheel zooms (scales the view).\n" +
                "    Shifted Scroll Wheel moves through sections.\n" +
                "    Left Click and Drag moves (pans) the view.\n" +
                "    Right Click switches to \"Modify Mode\" (Draw or Edit).\n" +
                "\n" +
                "  Draw Mode (without Segment/Bezier Drawing):\n" +
                "    Scroll Wheel zooms (scales the view).\n" +
                "    Shifted Scroll Wheel moves through sections.\n" +
                "    Left Click and Drag draws a new contour.\n" +
                "    Left Release completes the contour (ready to draw again).\n" +
                "    Right Click switches to \"Move Mode\".\n" +
                "\n" +
                "  Draw Mode (with Segment/Bezier Drawing):\n" +
                "    Scroll Wheel zooms (scales the view).\n" +
                "    Shifted Scroll Wheel moves through sections.\n" +
                "    Left Click adds a new segment point (repeat as needed).\n" +
                "    Right Click completes the contour (returns to Move Mode).\n" +
                "\n" +
                "  Edit Mode (toggled by shifted right click):\n" +
                "    Scroll Wheel zooms (scales the view).\n" +
                "    Shifted Scroll Wheel moves through sections.\n" +
                "    Left Click and drag moves a single point.\n" +
                "    Right Click returns to Move Mode.\n" +
                "\n";
      JOptionPane.showMessageDialog(null, s, "Reconstruct Java Mouse Operation", JOptionPane.INFORMATION_MESSAGE);
    } else if ( action_source == hot_keys_menu_item ) {
      String s =
                "Hot Keys:\n" +
                "\n" +
                "  A = Add a new point between closest point and its closest neighbor\n" +
                "\n" +
                "  D = Delete closest point\n" +
                "\n" +
                "  E = Edit Mode\n" +
                "\n" +
                "  N = New Trace Mode\n" +
                "\n";
      JOptionPane.showMessageDialog(null, s, "Reconstruct Java Hot Keys", JOptionPane.INFORMATION_MESSAGE);
    } else if ( action_source == edit_menu_item ) {
      current_cursor = Cursor.getPredefinedCursor ( Cursor.DEFAULT_CURSOR );
      setCursor ( current_cursor );
      center_draw = false;
      modify_mode = false;
      stroke_started = false;
      editing_mode = true;
      repaint();
    } else if ( action_source == move_menu_item ) {
      current_cursor = b_cursor;
      /*
      Alternative predefined cursors from java.awt.Cursor:
      current_cursor = Cursor.getPredefinedCursor ( Cursor.CROSSHAIR_CURSOR );
      current_cursor = Cursor.getPredefinedCursor ( Cursor.HAND_CURSOR );
      current_cursor = Cursor.getPredefinedCursor ( Cursor.MOVE_CURSOR );
      */
      setCursor ( current_cursor );
      //center_draw = false;
      modify_mode = false;
      stroke_started = false;
      editing_mode = false;
      repaint();
    } else if ( action_source == draw_menu_item ) {
      current_cursor = Cursor.getPredefinedCursor ( Cursor.CROSSHAIR_CURSOR );
      if (center_draw) {
        current_cursor = Cursor.getPredefinedCursor ( Cursor.HAND_CURSOR );
      }
      setCursor ( current_cursor );
      // center_draw = false;
      modify_mode = true;
      stroke_started = false;
      editing_mode = false;
      repaint();
    } else if ( action_source == center_draw_menu_item ) {
      JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
      center_draw = item.getState();
      if (modify_mode) {
        current_cursor = Cursor.getPredefinedCursor ( Cursor.HAND_CURSOR );
      }
      repaint();
    } else if ( action_source == segment_draw_menu_item ) {
      JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
      segment_draw = item.getState();
      if (segment_draw) {
        current_cursor = Cursor.getPredefinedCursor ( Cursor.HAND_CURSOR );
      } else {
        bezier_draw = false;
        bezier_draw_menu_item.setState ( bezier_draw );
      }
      repaint();
    } else if ( action_source == bezier_draw_menu_item ) {
      JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
      bezier_draw = item.getState();
      if (bezier_draw) {
        segment_draw = true;
        segment_draw_menu_item.setState ( segment_draw );
        current_cursor = Cursor.getPredefinedCursor ( Cursor.HAND_CURSOR );
      }
      repaint();
    } else if ( action_source == bezier_locked_menu_item ) {
      JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
      bezier_locked = item.getState();
      repaint();
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
          f.writeBytes ( ReconstructDefaults.convert_newlines ( ReconstructDefaults.default_series_file_string ) );
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
    } else if ( action_source == crlf_series_menu_item ) {
      file_chooser.setMultiSelectionEnabled(false);
      FileNameExtensionFilter filter = new FileNameExtensionFilter("Series Files", "ser");
      file_chooser.setFileFilter(filter);
      int returnVal = file_chooser.showDialog(this, "Convert Series to CRLF");
      if ( returnVal == JFileChooser.APPROVE_OPTION ) {
        File series_file = file_chooser.getSelectedFile();
        System.out.println ( "You chose to convert this series: " /* + chooser.getCurrentDirectory() + " / " */ + series_file );
        SeriesClass.convert_series_to_crlf ( series_file );
        repaint();
      }
    } else if ( action_source == lf_series_menu_item ) {
      file_chooser.setMultiSelectionEnabled(false);
      FileNameExtensionFilter filter = new FileNameExtensionFilter("Series Files", "ser");
      file_chooser.setFileFilter(filter);
      int returnVal = file_chooser.showDialog(this, "Convert Series to LF");
      if ( returnVal == JFileChooser.APPROVE_OPTION ) {
        File series_file = file_chooser.getSelectedFile();
        System.out.println ( "You chose to convert this series: " /* + chooser.getCurrentDirectory() + " / " */ + series_file );
        SeriesClass.convert_series_to_lf ( series_file );
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
    } else if ( action_source == series_options_menu_item ) {
      // Open the Series Options Dialog
      //JOptionPane.showMessageDialog(null, "Give new traces this name:", "Series Options", JOptionPane.WARNING_MESSAGE);
      String response = JOptionPane.showInputDialog(null, "Give new traces this name:", "Series Options", JOptionPane.QUESTION_MESSAGE);
      current_trace_name = null;
      if (response != null) {
        if (response.length() > 0) {
          current_trace_name = response;
          System.out.println ( "Series Options Current Trace Name = " + response );
        }
      }
      repaint();
    } else if ( action_source == line_menu_none_item ) {
      line_padding = -1; // This signals to not draw at all
      repaint();
    } else if ( action_source == line_menu_0_item ) {
      line_padding = 0;
      repaint();
    } else if ( action_source == line_menu_1_item ) {
      line_padding = 1;
      repaint();
    } else if ( action_source == line_menu_2_item ) {
      line_padding = 2;
      repaint();
    } else if ( action_source == sel_rad_1 ) {
      System.out.println ( "Setting selection radius to 1" );
      selection_radius = 1;
      repaint();
    } else if ( action_source == sel_rad_5 ) {
      System.out.println ( "Setting selection radius to 5" );
      selection_radius = 5;
      repaint();
    } else if ( action_source == sel_rad_10 ) {
      System.out.println ( "Setting selection radius to 10" );
      selection_radius = 10;
      repaint();
    } else if ( action_source == sel_rad_15 ) {
      System.out.println ( "Setting selection radius to 15" );
      selection_radius = 15;
      repaint();
    } else if ( action_source == show_points_menu_item ) {
      JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
      show_points = item.getState();
      repaint();
    } else if ( action_source == show_arrows_menu_item ) {
      JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
      show_arrows = item.getState();
      repaint();
    } else if ( action_source == show_handles_menu_item ) {
      JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
      show_handles = item.getState();
      repaint();
    } else if ( action_source == show_coords_menu_item ) {
      JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
      show_coords = item.getState();
      repaint();
    } else if ( action_source == show_glitch_menu_item ) {
      JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
      show_glitch = item.getState();
      System.out.println ( "Show Glitch = " + show_glitch );
      repaint();
    } else if ( action_source == glitch_options_menu_item ) {
      // Open the Glitch Options Dialog
      String response = JOptionPane.showInputDialog(null, "Enter Glitch Angle in degrees (" + glitch_angle_degrees + "):", "Glitch Options", JOptionPane.QUESTION_MESSAGE);
      if (response != null) {
        if (response.length() > 0) {
          glitch_angle_degrees = Double.parseDouble ( response );
          System.out.println ( "Glitch detection angle = " + glitch_angle_degrees );
        }
      }
      repaint();
    } else if ( action_source == export_handles_menu_item ) {
      JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
      export_handles = item.getState();
      repaint();
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
    } else if ( action_source == color_menu_red_item ) {
      new_trace_color=0xff0000;
      repaint();
    } else if ( action_source == color_menu_green_item ) {
      new_trace_color=0x00ff00;
      repaint();
    } else if ( action_source == color_menu_blue_item ) {
      new_trace_color=0x0000ff;
      repaint();
    } else if ( action_source == color_menu_yellow_item ) {
      new_trace_color=0xffff00;
      repaint();
    } else if ( action_source == color_menu_magenta_item ) {
      new_trace_color=0xff00ff;
      repaint();
    } else if ( action_source == color_menu_cyan_item ) {
      new_trace_color=0x00ffff;
      repaint();
    } else if ( action_source == color_menu_black_item ) {
      new_trace_color=0x000000;
      repaint();
    } else if ( action_source == color_menu_white_item ) {
      new_trace_color=0xffffff;
      repaint();
    } else if ( action_source == color_menu_half_item ) {
      // System.out.println ( "color_menu_half_item = " + color_menu_half_item );
      new_trace_color=0x00777777 & new_trace_color;
      repaint();
    } else if ( action_source == color_menu_light_item ) {
      int r = (new_trace_color & 0xff0000) >> 16;
      int g = (new_trace_color & 0x00ff00) >>  8;
      int b = (new_trace_color & 0x0000ff);
      r = 4 * r / 3; if (r > 255) r = 255;
      g = 4 * g / 3; if (g > 255) g = 255;
      b = 4 * b / 3; if (b > 255) b = 255;
      new_trace_color = (r << 16) + (g << 8) + b;
      repaint();
    } else if ( action_source == color_menu_dark_item ) {
      int r = (new_trace_color & 0xff0000) >> 16;
      int g = (new_trace_color & 0x00ff00) >>  8;
      int b = (new_trace_color & 0x0000ff);
      r = 3 * r / 4; if (r < 0) r = 0;
      g = 3 * g / 4; if (g < 0) g = 0;
      b = 3 * b / 4; if (b < 0) b = 0;
      new_trace_color = (r << 16) + (g << 8) + b;
      repaint();
    } else if ( action_source == purge_images_menu_item ) {
      if (this.series != null) {
        this.series.purge_images();
      }
      active_contour = null;
      repaint();
    } else if ( action_source == reverse_all_traces_menu_item ) {
      if (this.series != null) {
        this.series.reverse_all_strokes();
      }
      repaint();
    } else if ( action_source == clear_menu_item ) {
      if (this.series != null) {
        this.series.clear_strokes();
      }
      active_contour = null;
      repaint();
    } else if ( action_source == fix_menu_item ) {
      if (this.series != null) {
        this.series.fix_handles();
      }
      repaint();
    } else if ( action_source == import_images_menu_item ) {
      file_chooser.setMultiSelectionEnabled(true);
      FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "gif", "png", "tif", "tiff");
      file_chooser.setFileFilter(filter);
      int returnVal = file_chooser.showDialog(this, "Import Images");
      if ( returnVal == JFileChooser.APPROVE_OPTION ) {
        File image_files[] = file_chooser.getSelectedFiles();
        // Should have sorting options to properly order names like: xyz8.jpg xyz9.jpg xyz10.jpg xyz11.jpg
        System.out.println ( "You chose to import these " + image_files.length + " images:" );
        for (int i=0; i<image_files.length; i++) {
          System.out.println ( "  " + image_files[i] );
        }
        // Reconstruct.exe creates the section files as soon as the images have been imported so do it now
        this.series = new SeriesClass ( this.new_series_file_name );
        this.series.import_images ( image_files );
      }
    } else if ( action_source == add_section_menu_item ) {
      if (this.new_series_file_name == null) {
        System.out.println ( "Save as a new series first" );
        JOptionPane.showMessageDialog(null, "Save as a New series before adding sections.\nSeries / New...", "Note:", JOptionPane.WARNING_MESSAGE);
      } else {
        System.out.println ( "Making dummy sections" );
        this.series = new SeriesClass ( this.new_series_file_name );
        this.series.make_dummy_sections ( 5, 1024, 768 );
        bezier_draw = false;
        bezier_draw_menu_item.setState ( bezier_draw );
      }
    } else if ( action_source == list_sections_menu_item ) {
      System.out.println ( "Sections: ..." );
    } else if ( action_source == exit_menu_item ) {
      System.exit ( 0 );
    } else if (cmd.startsWith ( "Align" ) ) {
      System.out.println ( "Alignment command: " + cmd );
      String actual_cmd = swift.get_command ( cmd );
      System.out.println ( "Running actual command: \"" + actual_cmd + "\"" );
      swift.run_command ( actual_cmd );
      System.out.println ( "Done running actual command: \"" + actual_cmd + "\"" );
    } else {
      JOptionPane.showMessageDialog(null, "Option Not Implemented", "Option Not Implemented", JOptionPane.WARNING_MESSAGE);
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
      ReconstructDefaults.current_newline_string = "\r\n";  // CR+LF
    } else {
      ReconstructDefaults.current_newline_string = "\n";    // LF
    }

    System.out.println ( "Operating System is " + os_name );
    for (int i=0; i<args.length; i++) {
      System.out.println ( "Arg[" + i + "] = \"" + args[i] + "\"" );
    }

    // System.out.println ( "Reconstruct: Use the mouse wheel to zoom, and drag to pan." );
    javax.swing.SwingUtilities.invokeLater ( new Runnable() {
      public void run() {
        JFrame f = new JFrame("Reconstruct - No Active Series");
        f.setDefaultCloseOperation ( JFrame.EXIT_ON_CLOSE );

        Reconstruct zp = new Reconstruct();
        ReconstructDefaults.current_newline_string = "\n";  // For Windows this may be set to: "\r\n"

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
          debug_menu.add ( zp.add_section_menu_item = mi = new JMenuItem("New Empty Sections") );
          mi.addActionListener(zp);
        program_menu.add ( debug_menu );

        program_menu.add ( zp.exit_menu_item = mi = new JMenuItem("Exit") );
        mi.addActionListener(zp);
        menu_bar.add ( program_menu );

        JMenu series_menu = new JMenu("Series");
        series_menu.add ( mi = new JMenuItem("Open...") );
        zp.open_series_menu_item = mi;
        mi.addActionListener(zp);
        series_menu.add ( mi = new JMenuItem("Close") );
        mi.addActionListener(zp);

        series_menu.addSeparator();

        series_menu.add ( mi = new JMenuItem("New...") );
        zp.new_series_menu_item = mi;
        mi.addActionListener(zp);
        series_menu.add ( mi = new JMenuItem("Save") );
        zp.save_series_menu_item = mi;
        mi.addActionListener(zp);
        series_menu.add ( zp.series_options_menu_item = mi = new JMenuItem("Options...") );
        mi.addActionListener(zp);

        series_menu.addSeparator();

        series_menu.add ( mi = new JMenuItem("Convert to CRLF (Windows)") );
        zp.crlf_series_menu_item = mi;
        mi.addActionListener(zp);

        series_menu.add ( mi = new JMenuItem("Convert to LF (Unix)") );
        zp.lf_series_menu_item = mi;
        mi.addActionListener(zp);

        series_menu.addSeparator();

        JMenu export_menu = new JMenu("Export");
          export_menu.add ( mi = new JMenuItem("Images... ") );
          mi.addActionListener(zp);
          export_menu.add ( mi = new JMenuItem("Lines... ") );
          mi.addActionListener(zp);
          export_menu.add ( mi = new JMenuItem("Trace lists... ") );
          mi.addActionListener(zp);
        series_menu.add ( export_menu );

        JMenu import_menu = new JMenu("Import");
          import_menu.add ( zp.import_images_menu_item = mi = new JMenuItem("Images...") );
          mi.addActionListener(zp);
          import_menu.add ( mi = new JMenuItem("Lines...") );
          mi.addActionListener(zp);
          import_menu.add ( mi = new JMenuItem("Trace lists...") );
          mi.addActionListener(zp);
        series_menu.add ( import_menu );

        menu_bar.add ( series_menu );

        JMenu section_menu = new JMenu("Section");
        section_menu.add ( zp.list_sections_menu_item = mi = new JMenuItem("List Sections...") );
        mi.addActionListener(zp);
        section_menu.add ( mi = new JMenuItem("Thumbnails...") );
        mi.addActionListener(zp);

        section_menu.addSeparator();

        section_menu.add ( mi = new JMenuItem("New...") );
        mi.addActionListener(zp);
        section_menu.add ( mi = new JMenuItem("Save") );
        mi.addActionListener(zp);
        section_menu.add ( mi = new JMenuItem("Thickness...") );
        mi.addActionListener(zp);

        section_menu.addSeparator();

        section_menu.add ( mi = new JMenuItem("Undo") );
        mi.addActionListener(zp);
        section_menu.add ( mi = new JMenuItem("Redo") );
        mi.addActionListener(zp);
        section_menu.add ( mi = new JMenuItem("Reset") );
        mi.addActionListener(zp);
        section_menu.add ( mi = new JMenuItem("Blend") );
        mi.addActionListener(zp);

        JMenu zoom_menu = new JMenu("Zoom");
          zoom_menu.add ( mi = new JMenuItem("Center") );
          mi.addActionListener(zp);
          zoom_menu.add ( mi = new JMenuItem("Last") );
          mi.addActionListener(zp);
          zoom_menu.add ( mi = new JMenuItem("Actual pixels") );
          mi.addActionListener(zp);
          zoom_menu.add ( mi = new JMenuItem("Magnification...") );
          mi.addActionListener(zp);
        section_menu.add ( zoom_menu );

        JMenu movement_menu = new JMenu("Movement");
          movement_menu.add ( mi = new JMenuItem("Unlock") );
          mi.addActionListener(zp);
          JMenu flip_menu = new JMenu("Flip");
          flip_menu.add ( mi = new JMenuItem("Horizontally") );
          mi.addActionListener(zp);
          flip_menu.add ( mi = new JMenuItem("Vertically") );
          mi.addActionListener(zp);
          movement_menu.add ( flip_menu );
          JMenu rotate_menu = new JMenu("Rotate");
          rotate_menu.add ( mi = new JMenuItem("90 clockwise") );
          mi.addActionListener(zp);
          rotate_menu.add ( mi = new JMenuItem("90 counterclockwise") );
          mi.addActionListener(zp);
          rotate_menu.add ( mi = new JMenuItem("180") );
          mi.addActionListener(zp);
          movement_menu.add ( rotate_menu );
          movement_menu.add ( mi = new JMenuItem("Type in...") );
          mi.addActionListener(zp);
          movement_menu.addSeparator();
          movement_menu.add ( mi = new JMenuItem("By correlation") );
          mi.addActionListener(zp);
          movement_menu.addSeparator();
          movement_menu.add ( mi = new JMenuItem("Repeat") );
          mi.addActionListener(zp);
          movement_menu.add ( mi = new JMenuItem("Propagate...") );
          mi.addActionListener(zp);
          movement_menu.addSeparator();
          JMenu record_menu = new JMenu("Record");
          record_menu.add ( mi = new JMenuItem("Start") );
          mi.addActionListener(zp);
          record_menu.addSeparator();
          record_menu.add ( mi = new JMenuItem("from selected") );
          mi.addActionListener(zp);
          movement_menu.add ( record_menu );
        section_menu.add ( movement_menu );

        menu_bar.add ( section_menu );

        JMenu domain_menu = new JMenu("Domain");
          domain_menu.add ( mi = new JMenuItem("List image domains...") );
          mi.addActionListener(zp);
          domain_menu.add ( mi = new JMenuItem("Import image...") );
          mi.addActionListener(zp);
          domain_menu.addSeparator();
          domain_menu.add ( mi = new JMenuItem("Merge front") );
          mi.addActionListener(zp);
          domain_menu.add ( mi = new JMenuItem("Merge rear") );
          mi.addActionListener(zp);
          domain_menu.add ( mi = new JMenuItem("Attributes...") );
          mi.addActionListener(zp);
          domain_menu.add ( mi = new JMenuItem("Reinitialize ->") );
          mi.addActionListener(zp);
          domain_menu.addSeparator();
          domain_menu.add ( mi = new JMenuItem("Delete") );
          mi.addActionListener(zp);
          menu_bar.add ( domain_menu );


        JMenu trace_menu = new JMenu("Trace");
        trace_menu.add ( mi = new JMenuItem("List traces...") );
        mi.addActionListener(zp);
        trace_menu.add ( mi = new JMenuItem("Find...") );
        mi.addActionListener(zp);
        trace_menu.add ( mi = new JMenuItem("Select all") );
        mi.addActionListener(zp);
        trace_menu.add ( mi = new JMenuItem("Deselect all") );
        mi.addActionListener(zp);
        trace_menu.add ( mi = new JMenuItem("Zoom to") );
        mi.addActionListener(zp);
        trace_menu.addSeparator();
        trace_menu.add ( mi = new JMenuItem("Attributes...") );
        mi.addActionListener(zp);
        trace_menu.add ( mi = new JMenuItem("Palette...") );
        mi.addActionListener(zp);
        trace_menu.addSeparator();
        trace_menu.add ( mi = new JMenuItem("Cut") );
        mi.addActionListener(zp);
        trace_menu.add ( mi = new JMenuItem("Copy") );
        mi.addActionListener(zp);
        trace_menu.add ( mi = new JMenuItem("Paste") );
        mi.addActionListener(zp);
        trace_menu.add ( mi = new JMenuItem("Paste attributes") );
        mi.addActionListener(zp);
        trace_menu.add ( mi = new JMenuItem("Delete") );
        mi.addActionListener(zp);
        trace_menu.addSeparator();
        trace_menu.add ( mi = new JMenuItem("Align Section") );
        mi.addActionListener(zp);
        trace_menu.add ( mi = new JMenuItem("Calibrate...") );
        mi.addActionListener(zp);
        trace_menu.add ( mi = new JMenuItem("Merge") );
        mi.addActionListener(zp);
        trace_menu.add ( mi = new JMenuItem("Reverse") );
        mi.addActionListener(zp);
        trace_menu.add ( mi = new JMenuItem("Simplify") );
        mi.addActionListener(zp);
        trace_menu.add ( mi = new JMenuItem("Smooth") );
        mi.addActionListener(zp);
        trace_menu.addSeparator();
        trace_menu.add ( mi = new JMenuItem("Reverse All") );
        zp.reverse_all_traces_menu_item = mi;
        mi.addActionListener(zp);
        menu_bar.add ( trace_menu );

        JMenu object_menu = new JMenu("Object");
        object_menu.add ( mi = new JMenuItem("List Objects...") );
        mi.addActionListener(zp);
        object_menu.addSeparator();
        object_menu.add ( mi = new JMenuItem("3D Scene...") );
        mi.addActionListener(zp);
        object_menu.add ( mi = new JMenuItem("Z-Traces...") );
        mi.addActionListener(zp);
        object_menu.add ( mi = new JMenuItem("Distances...") );
        mi.addActionListener(zp);
        menu_bar.add ( object_menu );

        JMenu align_menu = new JMenu("Align");
        for (int i=0; i<zp.swift.num_commands(); i++) {
          String command_name = zp.swift.get_name(i);
          if (command_name.length() > 0) {
            if ( ! command_name.startsWith ( "Align" ) ) {
              System.out.println ( "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n Warning: Align command doesn't start with \"Align\"\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n" );
            }
            align_menu.add ( mi = new JMenuItem(command_name) );
            mi.addActionListener(zp);
          }
        }
        menu_bar.add ( align_menu );

        JMenu extras_menu = new JMenu("Extras");

          JMenu color_menu = new JMenu("Color");
            bg = new ButtonGroup();
            color_menu.add ( zp.color_menu_red_item = mi = new JRadioButtonMenuItem("Red", zp.new_trace_color==0xff0000) );
            mi.addActionListener(zp);
            bg.add ( mi );
            color_menu.add ( zp.color_menu_green_item = mi = new JRadioButtonMenuItem("Green", zp.new_trace_color==0x00ff00) );
            mi.addActionListener(zp);
            bg.add ( mi );
            color_menu.add ( zp.color_menu_blue_item = mi = new JRadioButtonMenuItem("Blue", zp.new_trace_color==0x0000ff) );
            mi.addActionListener(zp);
            bg.add ( mi );
            color_menu.add ( zp.color_menu_yellow_item = mi = new JRadioButtonMenuItem("Yellow", zp.new_trace_color==0xffff00) );
            mi.addActionListener(zp);
            bg.add ( mi );
            color_menu.add ( zp.color_menu_magenta_item = mi = new JRadioButtonMenuItem("Magenta", zp.new_trace_color==0xff00ff) );
            mi.addActionListener(zp);
            bg.add ( mi );
            color_menu.add ( zp.color_menu_cyan_item = mi = new JRadioButtonMenuItem("Cyan", zp.new_trace_color==0x00ffff) );
            mi.addActionListener(zp);
            bg.add ( mi );
            color_menu.add ( zp.color_menu_black_item = mi = new JRadioButtonMenuItem("Black", zp.new_trace_color==0x000000) );
            mi.addActionListener(zp);
            bg.add ( mi );
            color_menu.add ( zp.color_menu_white_item = mi = new JRadioButtonMenuItem("White", zp.new_trace_color==0xffffff) );
            mi.addActionListener(zp);
            bg.add ( mi );
          color_menu.addSeparator();
            color_menu.add ( zp.color_menu_half_item = mi = new JMenuItem("Half") );
            mi.addActionListener(zp);
            bg.add ( mi );
          color_menu.addSeparator();
            color_menu.add ( zp.color_menu_light_item = mi = new JMenuItem("Lighter") );
            mi.addActionListener(zp);
            bg.add ( mi );
            color_menu.add ( zp.color_menu_dark_item = mi = new JMenuItem("Darker") );
            mi.addActionListener(zp);
            bg.add ( mi );
        extras_menu.add ( color_menu );

          JMenu line_menu = new JMenu("Line");
            bg = new ButtonGroup();
            line_menu.add ( zp.line_menu_none_item = mi = new JRadioButtonMenuItem("None", zp.line_padding==-1) );
            mi.addActionListener(zp);
            bg.add ( mi );
            line_menu.add ( zp.line_menu_0_item = mi = new JRadioButtonMenuItem("Width = 1", zp.line_padding==0) );
            mi.addActionListener(zp);
            bg.add ( mi );
            line_menu.add ( zp.line_menu_1_item = mi = new JRadioButtonMenuItem("Width = 3", zp.line_padding==1) );
            mi.addActionListener(zp);
            bg.add ( mi );
            line_menu.add ( zp.line_menu_2_item = mi = new JRadioButtonMenuItem("Width = 5", zp.line_padding==2) );
            mi.addActionListener(zp);
            bg.add ( mi );
          line_menu.addSeparator();
          line_menu.add ( zp.show_points_menu_item = mi = new JCheckBoxMenuItem("Show Points", zp.show_points) );
          mi.addActionListener(zp);
          line_menu.add ( zp.show_arrows_menu_item = mi = new JCheckBoxMenuItem("Show Arrows", zp.show_arrows) );
          mi.addActionListener(zp);
          line_menu.add ( zp.show_handles_menu_item = mi = new JCheckBoxMenuItem("Show Handles", zp.show_handles) );
          mi.addActionListener(zp);
          line_menu.add ( zp.export_handles_menu_item = mi = new JCheckBoxMenuItem("Export Handles", zp.export_handles) );
          mi.addActionListener(zp);
          line_menu.add ( zp.show_coords_menu_item = mi = new JCheckBoxMenuItem("Show Coords", zp.show_coords) );
          mi.addActionListener(zp);
        extras_menu.add ( line_menu );

        extras_menu.addSeparator();

        extras_menu.add ( zp.show_glitch_menu_item = mi = new JCheckBoxMenuItem("Show Glitch", zp.show_glitch) );
        mi.addActionListener(zp);
        extras_menu.add ( zp.glitch_options_menu_item = mi = new JMenuItem("Glitch Options ...") );
        mi.addActionListener(zp);

        extras_menu.addSeparator();

        JMenu mode_menu = new JMenu("Mode");
          bg = new ButtonGroup();
          /*
          mode_menu.add ( zp.move_menu_item = mi = new JRadioButtonMenuItem("Move", !zp.modify_mode) );
          mi.addActionListener(zp);
          bg.add ( mi );
          mode_menu.add ( zp.draw_menu_item = mi = new JRadioButtonMenuItem("Draw", zp.modify_mode) );
          mi.addActionListener(zp);
          bg.add ( mi );
          mode_menu.add ( zp.edit_menu_item = mi = new JRadioButtonMenuItem("Edit", zp.editing_mode) );
          mi.addActionListener(zp);
          bg.add ( mi );
          mode_menu.addSeparator();
          */
          mode_menu.add ( zp.segment_draw_menu_item = new JCheckBoxMenuItem("Segment Drawing", zp.segment_draw) );
          zp.segment_draw_menu_item.addActionListener(zp);
          mode_menu.add ( zp.bezier_draw_menu_item = new JCheckBoxMenuItem("Bezier Drawing", zp.bezier_draw) );
          zp.bezier_draw_menu_item.addActionListener(zp);
          mode_menu.add ( zp.bezier_locked_menu_item = new JCheckBoxMenuItem("Beziers Aligned", zp.bezier_draw) );
          zp.bezier_locked_menu_item.addActionListener(zp);
          /*
          mode_menu.addSeparator();
          mode_menu.add ( zp.center_draw_menu_item = mi = new JCheckBoxMenuItem("Center Drawing", zp.center_draw) );
          mi.addActionListener(zp);
          */
          // mode_menu.add ( zp.dump_menu_item );
          // mi.addActionListener(zp);
          // mode_menu.add ( mi = new JMenuItem("Clear") );
          // mi.addActionListener(zp);
        extras_menu.add ( mode_menu );

        extras_menu.addSeparator();

        JMenu sel_radius_menu = new JMenu("Select Radius");
          JRadioButtonMenuItem rbi = null;
          bg = new ButtonGroup();
          sel_radius_menu.add ( zp.sel_rad_1 = rbi = new JRadioButtonMenuItem("1", false) );
          rbi.addActionListener(zp);
          bg.add ( rbi );
          sel_radius_menu.add ( zp.sel_rad_5 = rbi = new JRadioButtonMenuItem("5", true) );
          rbi.addActionListener(zp);
          bg.add ( rbi );
          sel_radius_menu.add ( zp.sel_rad_10 = rbi = new JRadioButtonMenuItem("10", false) );
          rbi.addActionListener(zp);
          bg.add ( rbi );
          sel_radius_menu.add ( zp.sel_rad_15 = rbi = new JRadioButtonMenuItem("15", false) );
          rbi.addActionListener(zp);
          bg.add ( rbi );
        extras_menu.add ( sel_radius_menu );

        extras_menu.addSeparator();

        extras_menu.add ( zp.purge_images_menu_item = mi = new JMenuItem("Purge Images") );
        mi.addActionListener(zp);

      menu_bar.add ( extras_menu );

        JMenu help_menu = new JMenu("Help");
        help_menu.add ( zp.draw_only_menu_item = mi = new JMenuItem("Draw Only...") );
        mi.addActionListener(zp);
        help_menu.addSeparator();
        help_menu.add ( zp.about_menu_item = mi = new JMenuItem("About...") );
        mi.addActionListener(zp);
        help_menu.add ( zp.menu_overview_menu_item = mi = new JMenuItem("Menu Items...") );
        mi.addActionListener(zp);
        help_menu.addSeparator();
        help_menu.add ( mi = new JMenuItem("Manual...") );
        mi.addActionListener(zp);
        help_menu.add ( mi = new JMenuItem("Key commands...") );
        mi.addActionListener(zp);
        help_menu.add ( zp.mouse_clicks_menu_item = mi = new JMenuItem("Mouse clicks...") );
        mi.addActionListener(zp);
        help_menu.add ( zp.hot_keys_menu_item = mi = new JMenuItem("Hot Keys...") );
        mi.addActionListener(zp);
        help_menu.addSeparator();
        help_menu.add ( mi = new JMenuItem("License...") );
        mi.addActionListener(zp);
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
