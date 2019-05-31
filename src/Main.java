/*******************************/
/* Main.java */

/* Name:    Andreas Charalampous
 * A.M :    1115201500195
 * e-mail:  sdi1500195@di.uoa.gr
 */
/********************************/
import syntaxtree.*;
import visitor.*;
import java.io.*;

public class Main {

    public static void main (String[] args){
        if(args.length < 1){
            System.err.println("Usage: java Main <inputFile1> <inputFile2> .. <inputFileN>");
            System.exit(1);
        }
        System.out.println("Valid execution. Performing Semantic Analysis on files given.");
        
        /* Perform Semantic Check on all files provided */
        for(int i = 0; i < args.length; i++){
            FileInputStream input_file = null;
            BufferedWriter  output_file = null;

            try{
                
                print_label(args[i]);

                input_file = new FileInputStream(args[i]);
                
                /* Parse file */
                MiniJavaParser parser = new MiniJavaParser(input_file);
                Goal root = parser.Goal();
                System.out.println("  -> Program Parsed Successfully.");
 
                SymbolTable symbol_table = new SymbolTable();
                
                /* Collect Class Names */ 
                CollectVisitor collect_v = new CollectVisitor(symbol_table);
                root.accept(collect_v, null);
        
                /* Check if any undeclared named found during class declarations */
                String err_type = symbol_table.check_unknown();
                if(err_type != null){
                    throw new Exception("Error: Unknown type " + err_type);
                }
                
                /* Perform Semantic Analysis */
                CheckVisitor check_v = new CheckVisitor(symbol_table);
                root.accept(check_v, null);
                System.out.println("  -> Program Symanticly Checked Succesfully.");
                
                /* Perform Lowering */
                LoweringST lst = new LoweringST();
                lst.fill_ST(symbol_table);
                
                output_file = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(get_output_filename(args[i])), "utf-8"));
                LoweringVisitor lower_v = new LoweringVisitor(lst, output_file);
                root.accept(lower_v, null);

                System.out.println("  -> Program Lowering finished Succesfully.");
                
            }
            catch(ParseException ex){
                System.out.println("\n\t** " + ex.getMessage());
            }
            catch(FileNotFoundException ex){
                System.err.println("\n\t** " + ex.getMessage());
            }
            catch(Exception error_msg){
                System.err.println("\n\t** " + error_msg.getMessage());
            }
            finally{
                try{
                    if(input_file != null) 
                        input_file.close();
                    if(output_file != null)
                        output_file.close();
                    }
                    catch(IOException ex){
                    System.err.println("\n\t** " + ex.getMessage());
                }
            }
        }
        System.out.println("\n");
    }


    /* Given a file name, print it surrounded by a box of asterisks */
    public static void print_label(String file_name){
        System.out.println("\n\n\n");
    
        /* Top Line */
        for(int j = 0; j < file_name.length() + 6; j++)
            System.out.print("*");
        System.out.flush();
    
        /* Middle line */
        System.out.println("\n** " + file_name + " **");
    
        /* Bottom Line */
        for(int j = 0; j < file_name.length() + 6; j++)
            System.out.print("*");
        System.out.flush();
    
    
        System.out.println("\n");
    }

    /* Given a .java file, checks if valid and returns its equivalent .ll file_name */
    public static String get_output_filename(String file_name) throws Exception{
        int file_name_len = file_name.length();
        if(file_name_len < 6 || !(file_name.substring(file_name_len - 5).equals(".java")))
            throw new Exception("Invalid file name extension: " + file_name);

        return file_name.substring(0, file_name_len - 5) + ".ll";
    }
}


