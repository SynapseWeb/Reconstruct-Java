/* This Class represents a Reconstruct Section. */

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
    String type_str = element.getAttribute("type");

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
      stroke.add ( 0, p );
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
        stroke_handles.add ( 0, h );
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
        System.out.println ( "Called init_bezier_and_close" );
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
    for (int j=0; j<stroke_points.size(); j++) {
      double p[] = stroke_points.get(j);
      priority_println ( 150, "   Contour Point " + j + " = [" + p[0] + "," + p[1] + "]" );
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
	  System.out.println ( "Called default_handle_points" );
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


	public void init_bezier ( boolean bezier ) {
		// Create the default Bezier handles for the current set of points

		System.out.println ( "Creating default Bezier handles" );

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
      // dump_contour("In init_bezier after adding handles");

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

        // Put a box around the first point
				if (r.show_points) {
				  double pfirst[] = stroke_points.get(0);
			    int x = r.x_to_pxi(pfirst[0]-dx);
			    int y = r.y_to_pyi(dy-pfirst[1]);
          g.setColor ( new Color ( 255, 255, 255 ) );
          int l=5;
				  g.drawLine ( x-l, y-l, x+l, y-l );
				  g.drawLine ( x-l, y-l, x-l, y+l );
				  g.drawLine ( x+l, y+l, x+l, y-l );
				  g.drawLine ( x+l, y+l, x-l, y+l );
				}

				int line_padding = r.line_padding;

				if (line_padding >= 0) {
				  // Only draw lines when line_padding >= 0

					if ( (is_bezier) && (handle_points != null) ) {

						Stroke previous_stroke = g2.getStroke();

						g.setColor ( new Color ( 100, 100, 100 ) );

						if (r.show_points && (r.show_handles)) {
						  // Draw the control handle lines
						  for (int j=0; j<handle_points.size(); j++) {
							  int x, y, hx, hy;
							  h = handle_points.get(j);
							  p = stroke_points.get(j);

							  x = r.x_to_pxi(p[0]-dx);
							  y = r.y_to_pyi(dy-p[1]);
							  hx = r.x_to_pxi(h[0][0]-dx);
							  hy = r.y_to_pyi(dy-h[0][1]);
							  g.setColor ( new Color ( 100, 0, 0 ) );
							  g.drawLine ( x, y, hx, hy );

							  x = r.x_to_pxi(p[0]-dx);
							  y = r.y_to_pyi(dy-p[1]);
							  hx = r.x_to_pxi(h[1][0]-dx);
							  hy = r.y_to_pyi(dy-h[1][1]);
							  g.setColor ( new Color ( 0, 100, 0 ) );
							  g.drawLine ( x, y, hx, hy );
						  }
						}

						g.setColor ( new Color ( 150, 150, 150 ) );

						if (r.show_points && (r.show_handles)) {
						  // Draw the control handle points
						  for (int j=0; j<handle_points.size(); j++) {
							  h = handle_points.get(j);
							  int l = 4;
							  int x, y;
							  x = r.x_to_pxi(h[0][0]-dx);
							  y = r.y_to_pyi(dy-h[0][1]);
							  g.setColor ( new Color ( 150, 0, 0 ) );
							  g.drawOval ( x-l, y-l, 2*l, 2*l );
							  x = r.x_to_pxi(h[1][0]-dx);
							  y = r.y_to_pyi(dy-h[1][1]);
							  g.setColor ( new Color ( 0, 150, 0 ) );
							  g.drawOval ( x-l, y-l, 2*l, 2*l );
						  }
					  }

						g.setColor ( new Color ( (int)(255*this.r), (int)(255*this.g), (int)(255*this.b) ) );

						if (r.show_points) {
						  // Draw the end points for the curves
						  for (int j=0; j<stroke_points.size(); j++) {
							  if (j == 0) {
								  g.setColor ( new Color ( 255, 255, 255 ) );
							  } else {
								  g.setColor ( new Color ( (int)(255*this.r), (int)(255*this.g), (int)(255*this.b) ) );
							  }
							  p0 = stroke_points.get(j);
							  int l = 4;
							  int x = r.x_to_pxi(p0[0]-dx);
							  int y = r.y_to_pyi(dy-p0[1]);
							  g.drawOval ( x-l, y-l, 2*l, 2*l );
						  }
						}


						double factor = 0.2;

						ArrayList<CubicCurve2D.Double> curves = new ArrayList<CubicCurve2D.Double>();  // Argument (if any) specifies initial capacity (default 10)
						// curves = new ArrayList<CubicCurve2D.Double>();  // Argument (if any) specifies initial capacity (default 10)

						p0 = translate_to_screen ( stroke_points.get(0), r );

						// path.moveTo ( r.x_to_pxi(p0[0]-dx), r.y_to_pyi(dy-p0[1]) );
						for (int j=1; j<stroke_points.size(); j++) {
							p1 = translate_to_screen ( stroke_points.get(j), r );

              // dump_contour("In drawing function before adding curves");

							if (true && handle_points.size() > j) {
							  // Attempt to draw from the handle_points array ... doesn't work yet
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
							  // Attempt to draw from the handle_points array ... doesn't work yet
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
								g.fillOval ( x-l, y-l, 2*l, 2*l );
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
                g.setColor ( new Color ( 255, 255, 255 ) );
                g.drawLine ( dxdydxdy[0]+(int)x, dxdydxdy[1]+(int)y, (int)x, (int)y );
                g.drawLine ( dxdydxdy[2]+(int)x, dxdydxdy[3]+(int)y, (int)x, (int)y );
							} else {
							  // Draw a box
                g.setColor ( new Color ( 255, 255, 255 ) );
                int l=8;
							  g.drawLine ( x-l, y-l, x+l, y-l );
							  g.drawLine ( x-l, y-l, x-l, y+l );
							  g.drawLine ( x+l, y+l, x+l, y-l );
							  g.drawLine ( x+l, y+l, x-l, y+l );
							}
						}
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

