/* This is a prototype of ArtAlign functionality. */
/* Add commands to the run_commands array below. */

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


public class AlignSWiFT {

  public static String current_newline_string = "\n";

  static String[] run_commands = {
    "Align: swim Help", "swim --help",
    "Align: iavg Help", "iavg",
    "Align", "ArtAlign",
    "Align 1", "ArtAlign 1",
    "Align 1 2", "ArtAlign 1 2",
    "Align 1 2 3", "ArtAlign 1 2 3",
    "Align ...", "ArtAlign 1.2 2.3 3.4 4.5 5.6 6.7 7.8 8.9",
    "", ""
  };

  static int num_commands() {
    return ( run_commands.length/2 );
  }

  static String get_name ( int i ) {
    return ( run_commands[2*i] );
  }

  static String get_command ( int i ) {
    return ( run_commands[(2*i)+1] );
  }

  static String get_command ( String name ) {
    int i=0;
    String command = null;
    while (i < run_commands.length/2 ) {
      if (name.equals(run_commands[2*i])) {
        command = run_commands[(2*i)+1];
        break;
      }
      i++;
    }
    return ( command );
  }

  static void run_command ( int i ) {
    run_command ( run_commands[(2*i)+1] );
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
      Process cmd_proc = rt.exec ( System.getenv("PWD") + File.separator + command );

      BufferedInputStream proc_out = new BufferedInputStream ( cmd_proc.getInputStream() );
      BufferedInputStream proc_err = new BufferedInputStream ( cmd_proc.getErrorStream() );

      cmd_proc.waitFor();

      System.out.println ( "Command Finished with " + proc_out.available() + " bytes in stream" );

      int num_left = 0;
      while ( ( num_left = proc_out.available() ) > 0 ) {
        byte b[] = new byte[num_left];
        proc_out.read ( b );
        System.out.println ( "Read: " + new String(b) );
      }

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
      AlignSWiFT.current_newline_string = "\r\n";  // CR+LF
    } else {
      AlignSWiFT.current_newline_string = "\n";    // LF
    }

    System.out.println ( "Operating System is " + os_name );
    for (int i=0; i<args.length; i++) {
      System.out.println ( "Arg[" + i + "] = \"" + args[i] + "\"" );
    }

  }

}

