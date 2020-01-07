/* This Class represents a Reconstruct Contour. */

/*
Contour points always exist in two places in memory:
  They exist as the XML "Element" data read in from the XML file (text).
  They exist as stroke points and handle points used for drawing (binary).
Since it's desirable to keep the text from being modified at all if there
is no change in the data (for git differencing), the text version will
always be written out if there has been no change to the data. However,
if the binary data DOES change, then the text data should be ignored.

The writing of binary data is complicated by the fact that Reconstruct
stores its points in the reverse order from which they were traced.
*/

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.Arrays;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import java.io.*;

import java.awt.image.*;
import javax.imageio.ImageIO;

import java.awt.geom.GeneralPath;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.awt.geom.*;


public class ContourClass {

  String contour_name = null;
  ArrayList<double[]> stroke_points = new ArrayList<double[]>();  // Argument (if any) specifies initial capacity (default 10)
  ArrayList<double[][]> handle_points = null;
  TransformClass xform = null;
  boolean closed = true;
  boolean hidden = false;
  double r=1.0, g=0.0, b=0.0;
  int mode=0;
  public boolean modified=true; // Any new contour can be considered "modified" because it needs to be saved
  public boolean is_bezier=false;
  Element contour_element=null;

  public ContourClass ( Element element, TransformClass current_transform ) {
    this.contour_element = element;
    this.contour_name = element.getAttribute("name");
    // System.out.println ( "  Importing " + this.contour_name );
    String type_str = element.getAttribute("type");

    if (element.hasAttribute("points")) {
      String points_str = element.getAttribute("points");
      String xy_str[] = points_str.trim().split(",");

      String handles_str = element.getAttribute("handles");
      String hxy_str[] = null;
      if (handles_str != null) {
        if (handles_str.trim().length() > 0) {
          System.out.println ( "======== Read a contour with handles!! =======" );
          hxy_str = handles_str.trim().split(",");
        }
      }

      // Allocate an ArrayList to hold the double points
      ArrayList<double[]> stroke = new ArrayList<double[]>(xy_str.length);
      for (int xyi=0; xyi<xy_str.length; xyi++) {
        String xy[] = xy_str[xyi].trim().split(" ");
        double p[] = { Double.parseDouble(xy[0]), Double.parseDouble(xy[1]) };
        stroke.add ( 0, p ); // Add at the front because Reconstruct store its points backwards!!
        priority_println ( 20, "              " + xy_str[xyi].trim() + " = " + p[0] + "," + p[1] );
      }
      // strokes.add ( stroke );
      is_bezier = false;
      if (type_str != null) {
        if (type_str.equals("bezier")) {
          is_bezier = true;
        }
      }

      ArrayList<double[]> stroke_handles = null;
      if (hxy_str != null) {
        is_bezier = true;
        // Allocate an ArrayList to hold the double handles
        stroke_handles = new ArrayList<double[]>(hxy_str.length);
        for (int xyi=0; xyi<hxy_str.length; xyi++) {
          String xy[] = hxy_str[xyi].trim().split(" ");
          double h[] = { Double.parseDouble(xy[0]), Double.parseDouble(xy[1]), Double.parseDouble(xy[2]), Double.parseDouble(xy[3]) };
          // stroke_handles.add ( h ); // Add at the end ... just because Reconstruct.exe stores its points backwards doesn't mean the handles must be also!!
          stroke_handles.add ( 0, h ); // Add at the front because Reconstruct store its points backwards!!
          priority_println ( 120, "              " + hxy_str[xyi].trim() + " = " + h[0] + "," + h[1] + "," + h[2] + "," + h[3] );
        }
      }

      // ContourClass cc = new ContourClass ( stroke, element.getAttribute("border"), element.getAttribute("closed").trim().equals("true"), is_bezier );

      this.init_bezier_and_closed ( stroke, stroke_handles, element.getAttribute("closed").trim().equals("true") );
      this.init_color ( element.getAttribute("border") );

      if (hxy_str == null) {
        this.init_bezier ( is_bezier );
      } else {
        this.is_bezier = true;
      }
    }

    set_mode ( Integer.parseInt ( element.getAttribute("mode").trim() ) );
    set_hidden ( element.getAttribute("hidden").trim().equals("true") );
    set_transform ( current_transform );
    modified = false; // This is currently a way to keep contours read from XML from being duplicated.
  }

  public ContourClass ( ArrayList<double[]> stroke, String color_string, boolean closed ) {
    this.init_stroke_and_closed ( stroke, closed );
    this.init_color ( color_string );
  }

  public ContourClass ( ArrayList<double[]> stroke, String color_string, boolean closed, boolean bezier ) {
    this.init_stroke_and_closed ( stroke, closed );
    this.init_color ( color_string );
    this.init_bezier ( bezier );
  }

  public ContourClass ( ArrayList<double[]> stroke, int trace_color, boolean closed ) {
    this.init_stroke_and_closed ( stroke, closed );
    this.init_color ( trace_color );
  }

  public ContourClass ( ArrayList<double[]> stroke, int trace_color, boolean closed, boolean bezier ) {
    this.init_stroke_and_closed ( stroke, closed );
    this.init_color ( trace_color );
    this.init_bezier ( bezier );
  }

  public void dump_contour(String location) {
    priority_println ( 150, "Contour at location: " + location );
    priority_println ( 150, " Contour ID:" + this );
    priority_println ( 150, "  name: " + this.contour_name );
    priority_println ( 150, "  stroke_points: " + this.stroke_points );
    if (stroke_points != null) {
      for (int j=0; j<stroke_points.size(); j++) {
        double p[] = stroke_points.get(j);
        priority_println ( 150, "   stroke_points[" + j + "] = [" + p[0] + "," + p[1] + "]" );
      }
    }
    priority_println ( 150, "  handle_points: " + this.handle_points );
    if (handle_points != null) {
      for (int j=0; j<handle_points.size(); j++) {
        double h[][] = handle_points.get(j);
        for (int hi=0; hi<h.length; hi++) {
          double p[] = h[hi];
          priority_println ( 150, "   handle_points[" + j + "][" + hi + "] = [" + p[0] + "," + p[1] + "]" );
        }
      }
    }
  }

  public void init_stroke_and_closed ( ArrayList<double[]> stroke, boolean closed ) {
    this.stroke_points = stroke;
    this.closed = closed;
  }

  public void init_bezier_and_closed ( ArrayList<double[]> stroke, ArrayList<double[]> stroke_handles, boolean closed ) {
    // Handle the normal stuff
    init_stroke_and_closed ( stroke, closed );

    // Handle the Bezier handles
    if (stroke_handles != null) {
      // stroke_handles is an ArrayList of double[4] where the values are h0x,h0y,h1x,h1y
      int n = stroke_handles.size();
      if (n > 0) {
        // dump_contour("Before init_bezier_and_closed loop");
        this.handle_points = new ArrayList<double[][]>();
        for (int handle_index=0; handle_index<n; handle_index++) {
          double sh[] = stroke_handles.get(handle_index);
          double h0[] = { sh[0], sh[1] };
          double h1[] = { sh[2], sh[3] };
          double hpts[][] = { h0, h1 };
          this.handle_points.add ( hpts );
        }
        // dump_contour("After init_bezier_and_closed loop");
      }
    }
  }

  public void init_color ( String color_string ) {
    String color_part_strings[] = color_string.trim().split(" ");
    try { r = Double.parseDouble ( color_part_strings[0].trim() ); } catch (Exception e) { r = 0.5; }
    try { g = Double.parseDouble ( color_part_strings[1].trim() ); } catch (Exception e) { g = 0.5; }
    try { b = Double.parseDouble ( color_part_strings[2].trim() ); } catch (Exception e) { b = 0.5; }
  }

  public void init_color ( int trace_color ) {
    r = ( (trace_color & 0x00ff0000) >> 16 ) / 255.0;
    g = ( (trace_color & 0x0000ff00) >>  8 ) / 255.0;
    b = ( (trace_color & 0x000000ff)       ) / 255.0;
  }


  public double twice_area() {
    double ha = 0.0;
    if (stroke_points != null) {
      int n = stroke_points.size();
      if (n >= 3) {
        // Area = 2 * Sum over each segment of (x1-x0)(y1+y0)
        double p0[] = null;
        double p1[] = null;
        p0 = stroke_points.get(0);
        for (int i=0; i<n; i++) {
          p1 = stroke_points.get((i+1)%n);
          ha += ( p1[0] - p0[0] ) * ( p1[1] + p0[1] );
          p0 = p1;
        }
      }
    }
    return ( ha );
  }

  public void dump_stroke() {
    dump_contour ( "dump_stroke" );
    priority_println ( 150, "=============" );
    for (int j=0; j<stroke_points.size(); j++) {
      double p[] = stroke_points.get(j);
      priority_println ( 150, "   Contour Point " + j + " = [" + p[0] + "," + p[1] + "]" );
    }
    if (handle_points != null) {
      for (int j=0; j<handle_points.size(); j++) {
        double hh[][] = handle_points.get(j);
        priority_println ( 150, "     Contour Handle[0] " + j + " = [" + hh[0][0] + "," + hh[0][1] + "]" );
        priority_println ( 150, "     Contour Handle[1] " + j + " = [" + hh[1][0] + "," + hh[1][1] + "]" );
      }
    }
    priority_println ( 150, "   Contour Area = " + (0.5 * twice_area()) );
  }

  public void dump_area() {
    String name = "";
    if (this.contour_name != null) {
      name = " " + this.contour_name;
    }
    priority_println ( 150, "   Contour" + name + ": Area = " + (0.5 * twice_area()) );
  }

  double[][] default_handle_points ( double p0[], double p1[] ) {
    double mid_x, mid_y;

    mid_x = p0[0] + ((p1[0]-p0[0])/3);
    mid_y = p0[1] + ((p1[1]-p0[1])/3);
    double h0[] = { mid_x, mid_y };

    mid_x = p0[0] + (2*(p1[0]-p0[0])/3);
    mid_y = p0[1] + (2*(p1[1]-p0[1])/3);
    double h1[] = { mid_x, mid_y };

    double h[][] = { h0, h1 };
    return ( h );
  }


  public void fix_handles() {
    if (is_bezier) {
      // Rearrange the handle points to fix an ordering problem
      int nh = handle_points.size();

      ArrayList<double[]> old_handle_points_list = new ArrayList<double[]>();
      for (int i=0; i<nh; i++) {
        double h[][] = handle_points.get(i);
        for (int j=0; j<2; j++) {
          old_handle_points_list.add ( h[j] );
        }
      }

      ArrayList<double[]> new_handle_points_list = new ArrayList<double[]>();
      for (int i=0; i<(2*nh); i++) {
        new_handle_points_list.add ( old_handle_points_list.get((i+(2*nh)-1)%(2*nh)) );
      }

      ArrayList<double[][]> rearranged_handle_points = new ArrayList<double[][]>();

      for (int i=0; i<nh; i++) {
        double h[][] = new double[2][2];
        h[0] = new_handle_points_list.get(2*i);
        h[1] = new_handle_points_list.get((2*i)+1);
        rearranged_handle_points.add ( h );
      }

      handle_points = rearranged_handle_points;
    }
  }


  public void init_bezier ( boolean bezier ) {
    // Create the default Bezier handles for the current set of points

    this.is_bezier = bezier;

    if ( (bezier) && (stroke_points.size() > 1) ) {
      // Compute the default handles for the points so far
      int n = stroke_points.size();
      handle_points = new ArrayList<double[][]>();

      double p0[] = null;
      double p1[] = null;

      // dump_contour("In init_bezier before adding handles");
      p0 = stroke_points.get(0);
      for (int stroke_point_index=1; stroke_point_index<n; stroke_point_index++) {
        p1 = stroke_points.get(stroke_point_index);
        handle_points.add ( default_handle_points ( p0, p1 ) );
        p0 = p1;
      }

      if (closed) {
        p1 = stroke_points.get(0);
        handle_points.add ( default_handle_points ( p0, p1 ) );
      }

      // Smooth the handles on all of the curves
      double factor = 0.2;
      int prev = 0;
      int next = 0;
      for (int i=0; i<n; i++) {
        prev = (n+i-1)%n;
        next = (n+i+1)%n;
        // double seg_to_adjust[][] =
        //// CubicCurve2D.Double seg_to_adjust = curves.get(i);
        // Adjust the h0 handle
        if ( (closed || (i > 0)) && (stroke_points.size() > i) && (handle_points.size() > i) ) {
          // System.out.println ( " Adjusting h0 for segment " + i + ", with previous = " + prev );

          double[] current_point = stroke_points.get(i);
          double[] next_point = stroke_points.get(next);
          double[] prev_point = stroke_points.get(prev);
          double cdx = next_point[0] - prev_point[0];
          double cdy = next_point[1] - prev_point[1];

          double[][]handles = handle_points.get(i);
          handles[0][0] = current_point[0] - (factor * cdx);
          handles[0][1] = current_point[1] - (factor * cdy);

          // These notes map the variables from BezierTracing.java to the CubicCurve2D members:
          // p0.x = x1
          // p0.y = y1
          // p1.x = x2
          // p1.y = y2
          // h0.x = ctrlx1
          // h0.y = ctrly1
          // h1.x = ctrlx2
          // h1.y = ctrly2
          // CubicCurve2D.Double(double x1, double y1, double ctrlx1, double ctrly1, double ctrlx2, double ctrly2, double x2, double y2)
          /*
          CubicCurve2D.Double prev_seg = curves.get((n+i-1)%n);
          double cdx = seg_to_adjust.x2 - prev_seg.x1;
          double cdy = seg_to_adjust.y2 - prev_seg.y1;
          seg_to_adjust.ctrlx1 = seg_to_adjust.x1 + (factor * cdx);
          seg_to_adjust.ctrly1 = seg_to_adjust.y1 + (factor * cdy);
          */
        }
        // Adjust the h1 handle
        if (closed || (i < n-1)) {
          // System.out.println ( " Adjusting h1 for segment " + i + ", with next = " + next );

          double[] current_point = stroke_points.get(i);
          double[] next_point = stroke_points.get(next);
          double[] prev_point = stroke_points.get(prev);
          double cdx = next_point[0] - prev_point[0];
          double cdy = next_point[1] - prev_point[1];

          double[][]handles = handle_points.get(i);
          handles[1][0] = current_point[0] + (factor * cdx);
          handles[1][1] = current_point[1] + (factor * cdy);


/*
          double[] current_point = stroke_points.get(i);
          double[] next_point = stroke_points.get(next);
          double cdx = next_point[0] - current_point[0];
          double cdy = next_point[1] - current_point[1];

          double[][]handles = handle_points.get(i);
          handles[1][0] = current_point[0] - (factor * cdx);
          handles[1][1] = current_point[1] - (factor * cdy);
*/
          /*
          CubicCurve2D.Double next_seg = curves.get((n+i+1)%n);
          double cdx = next_seg.x2 - seg_to_adjust.x1;
          double cdy = next_seg.y2 - seg_to_adjust.y1;
          seg_to_adjust.ctrlx2 = seg_to_adjust.x2 - (factor * cdx);
          seg_to_adjust.ctrly2 = seg_to_adjust.y2 - (factor * cdy);
          */
        }
      }

    } else {
      handle_points = null;
    }
  }



  public void draw_scaled_line ( Graphics g, Reconstruct r, int xoffset, int yoffset, double x0, double y0, double x1, double y1 ) {
    g.drawLine ( xoffset+r.x_to_pxi(x0),   yoffset+r.y_to_pyi(-y0),  xoffset+r.x_to_pxi(x1),  yoffset+r.y_to_pyi(-y1) );
  }

  double [] translate_to_screen ( double p[], Reconstruct r ) {
    double[] t = new double[2];
    double dx=0;
    double dy=0;
    if (this.xform != null) {
      if (this.xform.dim > 0) {
        dx = this.xform.xcoef[0];
        dy = this.xform.ycoef[0];
      }
    }
    t[0] = r.x_to_pxi(p[0]-dx);
    t[1] = r.y_to_pyi(dy-p[1]);
    return ( t );
  }

  CubicCurve2D.Double default_curve ( double p0[], double p1[] ) {
    double mid_x, mid_y;
    mid_x = p0[0] + ((p1[0]-p0[0])/3);
    mid_y = p0[1] + ((p1[1]-p0[1])/3);
    double h0[] = { mid_x, mid_y };
    //this.h0 = new CurvePoint ( mid_x, mid_y );
    mid_x = p0[0] + (2*(p1[0]-p0[0])/3);
    mid_y = p0[1] + (2*(p1[1]-p0[1])/3);
    double h1[] = { mid_x, mid_y };
    //this.h1 = new CurvePoint ( mid_x, mid_y );

    return ( new CubicCurve2D.Double ( p0[0], p0[1], h0[0], h0[1], h1[0], h1[1], p1[0], p1[1] ) );
  }

  public void draw ( Graphics g, Reconstruct r ) {

    if ( !hidden ) {
      Graphics2D g2 = (Graphics2D)g;
      double dx=0;
      double dy=0;
      if (this.xform != null) {
        if (this.xform.dim > 0) {
          dx = this.xform.xcoef[0];
          dy = this.xform.ycoef[0];
        }
      }

      if (stroke_points.size() > 0) {

        double p0[] = null;
        double p1[] = null;
        double h0[][] = null;
        double h1[][] = null;
        double h[][] = null;
        double p[] = null;

        int line_padding = r.line_padding;
        int point_radius = 4;

        // Put a box around the first point
        if (r.show_points) {
          double pfirst[] = stroke_points.get(0);
          int x = r.x_to_pxi(pfirst[0]-dx);
          int y = r.y_to_pyi(dy-pfirst[1]);
          g.setColor ( new Color ( 255, 255, 0 ) );
          int l=5;
          g.drawLine ( x-l, y-l, x+l, y-l );
          g.drawLine ( x-l, y-l, x-l, y+l );
          g.drawLine ( x+l, y+l, x+l, y-l );
          g.drawLine ( x+l, y+l, x-l, y+l );
        }

        if (line_padding >= 0) {
          // Only draw lines when line_padding >= 0

          if ( (is_bezier) && (handle_points != null) ) {

            Stroke previous_stroke = g2.getStroke();

            g.setColor ( new Color ( 100, 100, 100 ) );

            if (r.show_points && (r.show_handles)) {
              // Draw the control handle lines
              int num = handle_points.size();
              for (int j=0; j<num; j++) {
                int x, y, hx, hy;
                h = handle_points.get(j);
                p = stroke_points.get((j+num-1)%num);

                x = r.x_to_pxi(p[0]-dx);
                y = r.y_to_pyi(dy-p[1]);
                hx = r.x_to_pxi(h[0][0]-dx);
                hy = r.y_to_pyi(dy-h[0][1]);
                g.setColor ( new Color ( 100, 0, 0 ) );
                g.drawLine ( x, y, hx, hy );
                if (r.show_coords) {
                  String label = "handle=(" + (float)(h[0][0]) + "," + (float)(h[0][1]) + ")";
                  g.setColor ( new Color ( 200, 200, 200 ) );
                  g.drawString ( label, hx+point_radius, hy-point_radius );
                }

                h = handle_points.get(j);
                p = stroke_points.get(j);

                x = r.x_to_pxi(p[0]-dx);
                y = r.y_to_pyi(dy-p[1]);
                hx = r.x_to_pxi(h[1][0]-dx);
                hy = r.y_to_pyi(dy-h[1][1]);
                g.setColor ( new Color ( 0, 100, 0 ) );
                g.drawLine ( x, y, hx, hy );
                if (r.show_coords) {
                  String label = "handle=(" + (float)(h[1][0]) + "," + (float)(h[1][1]) + ")";
                  g.setColor ( new Color ( 200, 200, 200 ) );
                  g.drawString ( label, hx+point_radius, hy-point_radius );
                }
              }
            }

            g.setColor ( new Color ( 150, 150, 150 ) );

            if (r.show_points && (r.show_handles)) {
              // Draw the control handle points
              for (int j=0; j<handle_points.size(); j++) {
                h = handle_points.get(j);
                int x, y;
                x = r.x_to_pxi(h[0][0]-dx);
                y = r.y_to_pyi(dy-h[0][1]);
                if (r.active_point == h[0]) {
                  g.setColor ( new Color ( 255, 255, 255 ) );
                  //g.fillOval ( x-point_radius, y-point_radius, 2*point_radius, 2*point_radius );
                  g.fillOval ( x-(point_radius+1), y-(point_radius+1), 2*(point_radius+1), 2*(point_radius+1) );
                } else {
                  g.setColor ( new Color ( 150, 0, 0 ) );
                  g.drawOval ( x-point_radius, y-point_radius, 2*point_radius, 2*point_radius );
                  g.drawOval ( x-(point_radius+1), y-(point_radius+1), 2*(point_radius+1), 2*(point_radius+1) );
                }

                x = r.x_to_pxi(h[1][0]-dx);
                y = r.y_to_pyi(dy-h[1][1]);
                if (r.active_point == h[1]) {
                  g.setColor ( new Color ( 255, 255, 255 ) );
                  //g.fillOval ( x-point_radius, y-point_radius, 2*point_radius, 2*point_radius );
                  g.fillOval ( x-(point_radius+1), y-(point_radius+1), 2*(point_radius+1), 2*(point_radius+1) );
                } else {
                  g.setColor ( new Color ( 0, 150, 0 ) );
                  g.drawOval ( x-point_radius, y-point_radius, 2*point_radius, 2*point_radius );
                  g.drawOval ( x-(point_radius+1), y-(point_radius+1), 2*(point_radius+1), 2*(point_radius+1) );
                }
              }
            }

            g.setColor ( new Color ( (int)(255*this.r), (int)(255*this.g), (int)(255*this.b) ) );

            if (r.show_points) {
              // Draw the end points for the curves
              for (int j=0; j<stroke_points.size(); j++) {
                if (j == 0) {
                  g.setColor ( new Color ( 255, 255, 0 ) );
                } else {
                  g.setColor ( new Color ( (int)(255*this.r), (int)(255*this.g), (int)(255*this.b) ) );
                }
                p0 = stroke_points.get(j);
                int l = 4;
                int x = r.x_to_pxi(p0[0]-dx);
                int y = r.y_to_pyi(dy-p0[1]);
                if (r.active_point == p0) {
                  // System.out.println ( "Found active point" );
                  g.setColor ( new Color ( 255, 255, 255 ) );
                  //g.fillOval ( x-point_radius, y-point_radius, 2*point_radius, 2*point_radius );
                  g.fillOval ( x-(point_radius+1), y-(point_radius+1), 2*(point_radius+1), 2*(point_radius+1) );
                } else {
                  g.drawOval ( x-point_radius, y-point_radius, 2*point_radius, 2*point_radius );
                  g.drawOval ( x-(point_radius+1), y-(point_radius+1), 2*(point_radius+1), 2*(point_radius+1) );
                }
                if (r.show_coords) {
                  String label = "point=(" + (float)(p0[0]) + "," + (float)(p0[1]) + ")";
                  g.setColor ( new Color ( 200, 200, 200 ) );
                  g.drawString ( label, x+point_radius, y-point_radius );
                }
              }
            }

            g.setColor ( new Color ( (int)(255*this.r), (int)(255*this.g), (int)(255*this.b) ) );

            double factor = 0.2;

            ArrayList<CubicCurve2D.Double> curves = new ArrayList<CubicCurve2D.Double>();  // Argument (if any) specifies initial capacity (default 10)
            // curves = new ArrayList<CubicCurve2D.Double>();  // Argument (if any) specifies initial capacity (default 10)

            p0 = translate_to_screen ( stroke_points.get(0), r );

            // path.moveTo ( r.x_to_pxi(p0[0]-dx), r.y_to_pyi(dy-p0[1]) );
            for (int j=1; j<stroke_points.size(); j++) {
              p1 = translate_to_screen ( stroke_points.get(j), r );

              // dump_contour("In drawing function before adding curves");

              if (true && handle_points.size() > j) {
                // Draw from the handle_points array
                double[][] hh = handle_points.get(j);
                double[][] hht = new double[2][2];
                hht[0] = translate_to_screen ( hh[0], r );
                hht[1] = translate_to_screen ( hh[1], r );
                curves.add ( new CubicCurve2D.Double ( p0[0], p0[1], hht[0][0], hht[0][1], hht[1][0], hht[1][1], p1[0], p1[1] ) );
              } else {
                // Draw with "default" handles constructed on the fly
                curves.add ( default_curve ( p0, p1 ) );
              }

              // dump_contour("In drawing function after adding curves");

              p0 = p1;
            }

            if (closed) {
              p1 = translate_to_screen ( stroke_points.get(0), r );
              if (true) {
                // Draw from the handle_points array
                double[][] hh = handle_points.get(0);
                double[][] hht = new double[2][2];
                hht[0] = translate_to_screen ( hh[0], r );
                hht[1] = translate_to_screen ( hh[1], r );
                curves.add ( new CubicCurve2D.Double ( p0[0], p0[1], hht[0][0], hht[0][1], hht[1][0], hht[1][1], p1[0], p1[1] ) );
              } else {
                // Draw with "default" handles constructed on the fly
                curves.add ( default_curve ( p0, p1 ) );
              }
            }

            if (!closed) {
              // Smooth the handles on all of the curves
              int n = curves.size();
              for (int i=0; i<n; i++) {
                CubicCurve2D.Double seg_to_adjust = curves.get(i);
                // Adjust the h0 handle
                if (closed || (i > 0)) {
                  // System.out.println ( " Adjusting h0 for segment " + i + ", with previous = " + ((n+i-1)%n) );
                  CubicCurve2D.Double prev_seg = curves.get((n+i-1)%n);
                  double cdx = seg_to_adjust.x2 - prev_seg.x1;
                  double cdy = seg_to_adjust.y2 - prev_seg.y1;
                  seg_to_adjust.ctrlx1 = seg_to_adjust.x1 + (factor * cdx);
                  seg_to_adjust.ctrly1 = seg_to_adjust.y1 + (factor * cdy);
                }
                // Adjust the h1 handle
                if (closed || (i < n-1)) {
                  // System.out.println ( " Adjusting h1 for segment " + i + ", with next = " + ((n+i+1)%n) );
                  CubicCurve2D.Double next_seg = curves.get((n+i+1)%n);
                  double cdx = next_seg.x2 - seg_to_adjust.x1;
                  double cdy = next_seg.y2 - seg_to_adjust.y1;
                  seg_to_adjust.ctrlx2 = seg_to_adjust.x2 - (factor * cdx);
                  seg_to_adjust.ctrly2 = seg_to_adjust.y2 - (factor * cdy);
                }
              }
            }

            previous_stroke = g2.getStroke();
            if (line_padding >= 1) {
              g2.setStroke ( new BasicStroke(1 + (2*line_padding)) );
            }

            for (int j=0; j<curves.size(); j++) {
              g2.draw ( curves.get(j) );
            }

            g2.setStroke(previous_stroke);

          } else {

            g.setColor ( new Color ( (int)(255*this.r), (int)(255*this.g), (int)(255*this.b) ) );

            // System.out.println ( "Fill this contour when mode == " + this.mode + " ?" );
            GeneralPath path = new GeneralPath();
            p0 = stroke_points.get(0);
            path.moveTo ( r.x_to_pxi(p0[0]-dx), r.y_to_pyi(dy-p0[1]) );
            for (int j=1; j<stroke_points.size(); j++) {
              p0 = stroke_points.get(j);
              path.lineTo ( r.x_to_pxi(p0[0]-dx), r.y_to_pyi(dy-p0[1]) );
            }
            if (closed) {
              path.closePath();
            }

            // It's not clear what the "mode" means, but -13 seems to match objects to fill
            if (this.mode == -13) {
              g2.fill ( path );
            } else {
              Stroke previous_stroke = g2.getStroke();
              if (line_padding >= 1) {
                g2.setStroke ( new BasicStroke(1 + (2*line_padding)) );
              }
              g2.draw ( path );
              g2.setStroke(previous_stroke);
            }

            if (r.show_points) {
              int l = 4;
              int x, y;
              for (int j=0; j<stroke_points.size(); j++) {
                p0 = stroke_points.get(j);
                x = r.x_to_pxi(p0[0]-dx);
                y = r.y_to_pyi(dy-p0[1]);
                g.fillOval ( x-point_radius, y-point_radius, 2*point_radius, 2*point_radius );
              }
            }

          }

          // Draw arrows last so they're on top of everything else
          if (r.show_arrows) {
            int x=0, y=0, last_x=0, last_y=0;
            for (int j=0; j<stroke_points.size(); j++) {
              p0 = stroke_points.get(j);
              last_x = x;
              last_y = y;
              x = r.x_to_pxi(p0[0]-dx);
              y = r.y_to_pyi(dy-p0[1]);
              if (j > 0) {
                // Draw an arrow
                int dxdydxdy[] = r.arrows.get ( x-last_x, y-last_y );
                g.setColor ( new Color ( 200, 200, 200 ) );
                g.drawLine ( dxdydxdy[0]+(int)x, dxdydxdy[1]+(int)y, (int)x, (int)y );
                g.drawLine ( dxdydxdy[2]+(int)x, dxdydxdy[3]+(int)y, (int)x, (int)y );
              } else {
                // Draw a box
                g.setColor ( new Color ( 200, 200, 200 ) );
                int l=point_radius + 4;
                g.drawLine ( x-l, y-l, x+l, y-l );
                g.drawLine ( x-l, y-l, x-l, y+l );
                g.drawLine ( x+l, y+l, x+l, y-l );
                g.drawLine ( x+l, y+l, x-l, y+l );
              }
            }
          }

        }

        // Show and print "glitch" points when requested
        if (r.show_glitch) {
          // priority_println ( 150, "   Glitch!!" );
          double p2[] = null;
          double rp1[] = new double[2];
          double rp2[] = new double[2];
          double l1 = 0;
          double l2 = 0;
          double norm = 0;
          double angle = 0;
          boolean had_glitch = false;
          double glitch_angle = Math.PI * r.glitch_angle_degrees / 180;

          g.setColor ( new Color ( 200, 200, 255 ) );
          int l = stroke_points.size();
          //// TODO: Remove duplicate points which otherwise mask potential glitches
          for (int j=0; j<l; j++) {
            int prev_j = (j+l-1) % l;
            int next_j = (j+1) % l;
            p0 = stroke_points.get(j);
            p1 = stroke_points.get(prev_j);
            p2 = stroke_points.get(next_j);
            // System.out.println ( "Points: (" + p1[0] + "," + p1[1] + "), (" + p0[0] + "," + p0[1] + "), (" + p2[0] + "," + p2[1] + ")" );
            // Translate coordinates of p1 and p2 to place origin at p0
            rp1[0] = p1[0]-p0[0];
            rp1[1] = p1[1]-p0[1];
            rp2[0] = p2[0]-p0[0];
            rp2[1] = p2[1]-p0[1];
            // Calculate norms
            l1 = Math.sqrt ( (rp1[0]*rp1[0]) + (rp1[1]*rp1[1]) );
            l2 = Math.sqrt ( (rp2[0]*rp2[0]) + (rp2[1]*rp2[1]) );
            norm = l1 * l2;
            if (norm != 0) {
              angle = Math.acos(((rp1[0]*rp2[0])+(rp1[1]*rp2[1]))/(l1*l2));
              if (Math.abs(angle) < glitch_angle) {
                had_glitch = true;
                priority_println ( 150, "Glitch Point in " + this.contour_name + " at (" + p0[0] + "," + p0[1] + ")" );
                int x = r.x_to_pxi(p0[0]-dx);
                int y = r.y_to_pyi(dy-p0[1]);
                g.drawOval ( x-30, y-30, 2*30, 2*30 );
              }
            }
          }
          if (had_glitch) {
            priority_println ( 150, "" );
          }
        }

      }
    }
  }

  public double[] find_closest( double p[] ) {
    // System.out.println ( "Contour looking for closest to " + p[0] + ", " + p[1] );
    double closest[] = null;
    double closest_dist_sq = Double.MAX_VALUE;
    if (stroke_points != null) {
      for (int i=0; i<stroke_points.size(); i++) {
        double stroke_point[] = stroke_points.get(i);
        double dx = p[0]-stroke_point[0];
        double dy = p[1]-stroke_point[1];
        double dist_sq = (dx*dx) + (dy*dy);
        // System.out.println ( "Checking " + stroke_point[0] + ", " + stroke_point[1] );
        if ( (closest == null) || (dist_sq < closest_dist_sq) ) {
          closest = stroke_point;
          closest_dist_sq = dist_sq;
        }
      }
    }
    if (handle_points != null) {
      for (int i=0; i<handle_points.size(); i++) {
        double[][]handles = handle_points.get(i);
        for (int h=0; h<2; h++) {
          double dx = p[0]-handles[h][0];
          double dy = p[1]-handles[h][1];
          double dist_sq = (dx*dx) + (dy*dy);
          // System.out.println ( "Checking " + handles[h][0] + ", " + handles[h][1] );
          if ( (closest == null) || (dist_sq < closest_dist_sq) ) {
            closest = handles[h];
            closest_dist_sq = dist_sq;
          }
        }
      }
    }
    return ( closest );
  }

  public int find_closest_index( double p[] ) {
    // System.out.println ( "Contour looking for closest to " + p[0] + ", " + p[1] );
    int closest_index = -1;
    double closest[] = null;
    double closest_dist_sq = Double.MAX_VALUE;
    if (stroke_points != null) {
      for (int i=0; i<stroke_points.size(); i++) {
        double stroke_point[] = stroke_points.get(i);
        double dx = p[0]-stroke_point[0];
        double dy = p[1]-stroke_point[1];
        double dist_sq = (dx*dx) + (dy*dy);
        // System.out.println ( "Checking " + stroke_point[0] + ", " + stroke_point[1] );
        if ( (closest == null) || (dist_sq < closest_dist_sq) ) {
          closest_index = i;
          closest = stroke_point;
          closest_dist_sq = dist_sq;
        }
      }
    }
    return ( closest_index );
  }

  public void insert_point (  int closest_i, double[] p ) {
    System.out.println ( "Contour inserting point (" + p[0] + "," + p[1] + ") at " + closest_i );
    if (handle_points != null) {
      System.out.println ( "Warning: Cannot insert points into a Bezier curve yet." );
      JOptionPane.showMessageDialog(null, "Cannot insert points into a Bezier curve yet.", "Warning:", JOptionPane.WARNING_MESSAGE);
    } else {
      if ( closest_i < stroke_points.size() ) {
        stroke_points.add ( closest_i, p );
      }
    }
  }

  public void insert_point_in_line (  int closest_i, double[] p ) {
    System.out.println ( "Contour inserting point (" + p[0] + "," + p[1] + ") in line at " + closest_i );
    if (handle_points != null) {
      System.out.println ( "Warning: Cannot insert points into a Bezier curve yet." );
      JOptionPane.showMessageDialog(null, "Cannot insert points into a Bezier curve yet.", "Warning:", JOptionPane.WARNING_MESSAGE);
    } else {
      // This should be a more complex algorithm to find the best place to insert a new point
      // But for now, just use the closest.
      int n = stroke_points.size();
      System.out.println ( "Contour contains " + n + " points." );
      if ( (closest_i >= 0) && (closest_i < n) ) {
        // The value of closest_i is a legal index in stroke_points
        int insert_at = 0;
        if (n == 0) {
          // There are no points, so just add this one
          insert_at = 0;
        } else if (n == 1) {
          // There is only one point, so add this one as the second (there is no "direction")
          insert_at = 1;
        } else if (n == 2) {
          // There are only two points, so add this one as the third (makes a loop either way)
          insert_at = 2;
        } else {
          // There are more than two points in the array, so find the best location
          int before = closest_i - 1;
          int after = closest_i + 1;
          while (before < 0) before = before + n;
          while (after >= n) after  = after  - n;

          System.out.println ( "Point indexes: " + before + ", " + closest_i + ", " + after );

          double[] pc = stroke_points.get(closest_i);
          double[] pb = stroke_points.get(before);
          double[] pa = stroke_points.get(after);
          double dx, dy;

          dx = pb[0] - p[0];
          dy = pb[1] - p[1];
          double dist_pb_p = Math.sqrt ( (dx*dx) + (dy*dy) );

          dx = pb[0] - pc[0];
          dy = pb[1] - pc[1];
          double dist_pb_pc = Math.sqrt ( (dx*dx) + (dy*dy) );

          if (dist_pb_p < dist_pb_pc) {
            insert_at = closest_i;
          } else {
            insert_at = closest_i + 1;
          }
        }
        stroke_points.add ( insert_at, p );
      }
    }
  }

  public void delete_point ( int i ) {
    System.out.println ( "Contour deleting point " + i );
    if (handle_points != null) {
      System.out.println ( "Warning: Cannot delete points from a Bezier curve yet." );
      JOptionPane.showMessageDialog(null, "Cannot delete points from a Bezier curve yet.", "Warning:", JOptionPane.WARNING_MESSAGE);
    } else {
      if ( i < stroke_points.size() ) {
        stroke_points.remove ( i );
      }
    }
  }

  public double[][] find_bezier_triplet ( double query_point[] ) {
    // This function is passed a query point that should be either a contour point or a contour handle
    // This function returns an array of 3 points: [ h0, p, h1 ] where one of those points matches the original query point
    // System.out.println ( "Contour looking for bezier triplet for " + p[0] + ", " + p[1] );
    if (stroke_points != null) {
      for (int i=0; i<stroke_points.size(); i++) {
        double stroke_point[] = stroke_points.get(i);
        if (stroke_point == query_point) {
          // System.out.println ( "Query point matched contour point " + i  );
          double triplet[][] = new double[3][2];
          triplet[0] = null;
          triplet[1] = stroke_point;
          triplet[2] = null;
          if (handle_points != null) {
            double[][]prev_handles = handle_points.get(i);
            double[][]next_handles = handle_points.get((i+1)%handle_points.size());
            triplet[0] = prev_handles[1];
            triplet[2] = next_handles[0];
          }
          return ( triplet );
        }
      }
    }
    if (handle_points != null) {
      for (int i=0; i<handle_points.size(); i++) {
        double[][]handles = handle_points.get(i);
        for (int h=0; h<2; h++) {
          if (handles[h] == query_point) {
            // System.out.println ( "Query point matched handle point " + i + "." + h );
            double triplet[][] = new double[3][2];
            triplet[0] = null;
            triplet[1] = null;
            triplet[2] = null;
            if (h == 0) {
              int prev_index = (i+handle_points.size()-1)%handle_points.size();
              triplet[0] = handle_points.get(prev_index)[1];
              triplet[1] = stroke_points.get(prev_index);
              triplet[2] = handle_points.get(i)[0];
            } else if (h == 1) {
              int next_index = (i+1)%handle_points.size();
              triplet[0] = handle_points.get(i)[1];
              triplet[1] = stroke_points.get(i);
              triplet[2] = handle_points.get(next_index)[0];
            }
            return ( triplet );
          }
        }
      }
    }
    return ( null );
  }


  public void reverse_stroke() {
    System.out.println ( "   Contour reversing stroke" );
    ArrayList<double[]> reversed_stroke_points = new ArrayList<double[]>();
    for (int j=stroke_points.size()-1; j>=0; j--) {
      double p[] = stroke_points.get(j);
      reversed_stroke_points.add ( p );
    }
    stroke_points = reversed_stroke_points;
  }

  public void clear_strokes() {
    stroke_points = new ArrayList<double[]>();
  }

  public void close() {
    closed = true;
  }

  public void add_point (	double[] point ) {
    stroke_points.add ( point );
  }

  public void set_mode ( int mode ) {
    this.mode = mode;
  }

  public void set_hidden ( boolean hidden ) {
    this.hidden = hidden;
  }

  public void set_transform ( TransformClass xform ) {
    this.xform = xform;
  }

  static void priority_println ( int thresh, String s ) {
    if (thresh >= 90) {
      System.out.println ( s );
    }
  }

  public static void main ( String[] args ) {
    priority_println ( 50, "Testing ContourClass.java ..." );
  }

}

