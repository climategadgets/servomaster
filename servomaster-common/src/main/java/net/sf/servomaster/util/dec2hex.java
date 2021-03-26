package net.sf.servomaster.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;

/**
 * This class takes a comma separated stream of decimal numbers and prints
 * the hexadecimal equivalents of them in neat rows of 8. The result is
 * printed on the standard out.
 *
 * <p>
 *
 * Since this class is primitive, it will not tolerate any alien elements in
 * the input stream.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2018
 */
public class dec2hex {

    public static void main(String[] args) {

        try {

            if ( args.length == 0 ) {

                System.err.println("Usage: dec2hex <input file>");
            }

            BufferedReader br = new BufferedReader(new FileReader(args[0]));

            int offset = 0;

            while ( true ) {

                String line = br.readLine();

                if ( line == null ) {

                    break;
                }

                for ( StringTokenizer st = new StringTokenizer(line, ",\n"); st.hasMoreTokens(); ) {

                    if (offset++ % 8 == 0) {

                        System.out.println("");
                    }

                    String token = st.nextToken();
                    int value = Integer.parseInt(token);
                    String hexValue = Integer.toHexString(value);

                    if ( hexValue.length() == 1 ) {

                        hexValue = "0" + hexValue;
                    }

                    System.out.print("(byte)0x" + hexValue + ", ");
                }
            }
            
            br.close();

        } catch ( Throwable t ) {

            System.err.println("Oops...");
            t.printStackTrace();
        }
    }

}
