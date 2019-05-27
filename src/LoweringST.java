/*******************************/
/* LoweringST.java */

/* Name:    Andreas Charalampous
 * A.M :    1115201500195
 * e-mail:  sdi1500195@di.uoa.gr
 */
/********************************/

import java.io.BufferedWriter;
import java.util.*;


/* Implementation of the Symbol Table that it will be used for Lowering to LLVM from MiniJava */
public class LoweringST{
    private Map<String, ClassOffsets> classes; // maps class names with their offset info

    private Map<String, NameType> current_scope; // Holds all the declared variables with their register name + type, in current scope
 
    private int register_counter; // number of the next register
    private int arr_alloc;

    public LoweringST(){
        this.classes = new LinkedHashMap<>();
        this.current_scope = null;
    }

    /* Given a symbol table after semantic check, collect all offsets and fill st */
    public void fill_ST(SymbolTable st){
        Map<String, ClassContent> class_names = st.get_class_names();
        Map<String, Vector<String>> subtypes = st.get_subtypes();

        /* Fill every Class' offsets */
        for (Map.Entry<String, ClassContent> entry : class_names.entrySet()){
            ClassOffsets co = new ClassOffsets();
            String class_name = entry.getKey();
            
            /* Start from their supers */
            Vector<String> super_classes = subtypes.get(class_name);
            if(super_classes != null){
                for(int i = super_classes.size() - 1; i >= 0; i--){
                    ClassContent cc = class_names.get(super_classes.elementAt(i));
                    co.fill_offsets(super_classes.elementAt(i), cc);
                }    
            }

            ClassContent cc = class_names.get(class_name);
            co.fill_offsets(class_name, cc);

            this.classes.put(class_name, co);
        }
    }

    /* Prints the global vtables at the start of the given output_file */
    public void print_vtables(BufferedWriter output_file) throws Exception{
        /* For every class print its vtable */
        for (Map.Entry<String, ClassOffsets> entry : this.classes.entrySet()){
            String class_name = entry.getKey();
            ClassOffsets co = entry.getValue();

            int vt_size = co.get_vt_size();

            output_file.write("@." + class_name + "_vtable = global [" + vt_size + " x i8*] [");

            int count = 0;
            /* Declare each method and it's types in vtable */
            for (Map.Entry<String, MethodInfo> m_entry : entry.getValue().get_methods().entrySet()){
                count++;
                
                String m_name = m_entry.getKey();
                MethodInfo m_info = m_entry.getValue();

                String m_class_name = m_info.get_class_name();
                output_file.write("i8* bitcast (");

                /* Print return type */
                Vector<NameType> types = m_info.get_types();
                String ret_type = types.elementAt(0).get_type();
                String ret_type_llvm = get_llvm_type(ret_type);

                output_file.write(ret_type_llvm + " (i8*");

                /* Print every argument type */
                for(int i = 1; i < types.size(); i++){
                    output_file.write("," + get_llvm_type(types.elementAt(i).get_type()));
                }
                output_file.write(")* @" + m_class_name + "." + m_name + " to i8*)");

                /* If not the last method in vtable, place comma for next method */
                if(count != vt_size)
                    output_file.write(", ");
            } // end methods print
            output_file.write("]\n");
        } // end class' vtable print
    }
    
    public String get_llvm_type(String java_type){
        if(java_type.equals("boolean")) return "i1";
        else if(java_type.equals("int")) return "i32";
        else if(java_type.equals("int[]")) return "i32*";
        else return "i8*";
    }

    public void print_ext_methods(BufferedWriter output_file) throws Exception{
        output_file.write(  
            "\n\ndeclare i8* @calloc(i32, i32)\ndeclare i32 @printf(i8*, ...)\ndeclare void @exit(i32)" +
            "\n\n@_cint = constant [4 x i8] c\"%d\\0a\\00\"" +
            "\n@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"" +
            "\ndefine void @print_int(i32 %i) {" +
            "\n\t%_str = bitcast [4 x i8]* @_cint to i8*" +
            "\n\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)" +
            "\n\tret void " +
            "\n} " +
            "\n\ndefine void @throw_oob() { " +
            "\n\t%_str = bitcast [15 x i8]* @_cOOB to i8* " +
            "\n\tcall i32 (i8*, ...) @printf(i8* %_str) " +
            "\n\tcall void @exit(i32 1) " +
            "\n\tret void" +
            "\n}"
        );
    }

    
    /* Insert variable in current scope. Keep it's register name and type */
    public void insert(String name, String reg_name, String type){
        this.current_scope.put(name, new NameType(reg_name, type));
    }


    public String get_var_reg(String var_name){
        return this.current_scope.get(var_name).get_name();
    }

    public int get_var_offset(String class_name, String variable_name){
        return this.classes.get(class_name).get_variables().get(variable_name).get_offset();
    }
    
    public void enter_scope(){
        this.current_scope = new HashMap<>();
        this.register_counter = 0;
        this.arr_alloc = 0;
    }
    
    public void exit_scope(){
        this.current_scope = null;
    }

    public void print_all(){
        for (Map.Entry<String, ClassOffsets> entry : this.classes.entrySet()){
            System.out.println("\n\n" + entry.getKey());
            entry.getValue().print_all();
        }
    }

    public String get_register(){
        String num = Integer.toString(this.register_counter);
        this.register_counter++;
        return "%_" + num;
    }

    public String get_arr_label(){
        String num = Integer.toString(this.arr_alloc);
        this.arr_alloc++;
        return "arr_alloc" + num;
    }

    /* Accessors */
    public Map<String, ClassOffsets> get_classes(){ return this.classes; } 
}