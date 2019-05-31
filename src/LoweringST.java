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
    private Map<String, String> object_class; // Maps each instance variable to its type

    /* Labels */
    private int register_counter; // number of the next register
    private int arr_alloc_lbl; // number of the next label, for array size check, during new int[]
    private int oob_lbl; // number of the next label, for array bounds check, durign array lookup
    private int and_lbl; // number of the next label, for andclause
    private int if_lbl; // number for the next if label
    private int loop_lbl; // number for the next loop label
    
    private Vector<NameType> method_decl_args; // Maps all method's argument with their type during method declaration

    /* Vector that keeps vectors of arguments, for each level of inner method calls(method call as argument to another call) */
    private Vector<Vector<String>> method_call_args; 

    /* Vector that keeps registers and their type, containing the value of arguments of each inner method call */    
    private Vector<Vector<NameType>> method_call_regs;


    /* Constructor */
    public LoweringST(){
        this.classes = new LinkedHashMap<>();
        this.current_scope = null;
        this.method_call_args = new Vector<>();
        this.method_call_regs = new Vector<>();

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
    
    /* Return llvm type equivalent to the java type given */
    public String get_llvm_type(String java_type){
        if(java_type.equals("boolean")) return "i1";
        else if(java_type.equals("int")) return "i32";
        else if(java_type.equals("int[]")) return "i32*";
        else return "i8*";
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

    /* Print at start of llvm file */
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


    ///////////////////
    /* CURRENT-SCOPE */
    ///////////////////
    public void enter_scope(){
        this.current_scope = new HashMap<>();
        this.object_class = new HashMap<>();
        this.register_counter = 0;
        this.arr_alloc_lbl = 0;
        this.oob_lbl = 0;
        this.and_lbl = 0;
        this.loop_lbl = 0;
        this.if_lbl = 0;
    }
    
    public void exit_scope(){ this.current_scope = null; }

    /* Insert variable in current scope. Keep it's register name and type */
    public void insert(String name, String reg_name, String type){ this.current_scope.put(name, new NameType(reg_name, type)); }

    /* Get type of variable given in current scope */
    public String lookup(String var_name){
        NameType co = this.current_scope.get(var_name);
        if(co != null)
            return co.get_type();
        else
        return null;
    }

    /* Get register name of variable given, in current scope */
    public String get_var_reg(String var_name){
        NameType co = this.current_scope.get(var_name);
        if(co != null)
            return co.get_name();
        else
        return null;
    }
    
    
    ////////////////////////
    /* OBJECT - CLASS MAP */
    ////////////////////////
    /* Insert object-instance and its type in object map */ 
    public void insert_object(String var, String type){ this.object_class.put(var, type); }
    /* Get object-instance type */
    public String get_object_type(String var){ return this.object_class.get(var); }
    
    
    /////////////////////
    /* CLASSES OFFSETS */
    /////////////////////
    public Map<String, ClassOffsets> get_classes(){ return this.classes; } 
    /* Get given class' variable offset */
    public int get_var_offset(String class_name, String variable_name){ return this.classes.get(class_name).get_variables().get(variable_name).get_offset(); }
    /* Get given class' variable type */    
    public String get_var_type(String class_name, String variable_name){ return this.classes.get(class_name).get_variables().get(variable_name).get_type(); }
    /* Get given class' method offset */
    public int get_method_offset(String class_name, String method_name){ return this.classes.get(class_name).get_methods().get(method_name).get_offset(); }
    

    ////////////
    /* LABELS */
    ////////////
    public String get_register(){
        String num = Integer.toString(this.register_counter);
        this.register_counter++;
        return "%_" + num;
    }

    public String get_arr_label(){
        String num = Integer.toString(this.arr_alloc_lbl);
        this.arr_alloc_lbl++;
        return "arr_alloc" + num;
    }
    
    public String get_oob_label(){
        String num = Integer.toString(this.oob_lbl);
        this.oob_lbl++;
        return "oob" + num;
    }
    
    
    public String get_and_label(){
        String num = Integer.toString(this.and_lbl);
        this.and_lbl++;
        return "andclause" + num;
    }
    
    public String get_if_label(){
        String num = Integer.toString(this.if_lbl);
        this.if_lbl++;
        return "if" + num;
    }
    
    public String get_loop_label(){
        String num = Integer.toString(this.loop_lbl);
        this.loop_lbl++;
        return "loop" + num;
    }
    
    
    public void print_all(){
        for (Map.Entry<String, ClassOffsets> entry : this.classes.entrySet()){
            System.out.println("\n\n" + entry.getKey());
            entry.getValue().print_all();
        }
    }


    /////////////////////////////
    /* Method Declaration Args */
    /////////////////////////////

    public Vector<NameType> get_mdecl_args(){ return this.method_decl_args; }
    
    /* Initializes method_decl_args for a new method */
    public void mdecl_args_init(){ method_decl_args = new Vector<>(); }
    
    /* Adds new argument to current method's arguments */
    public void mdecl_args_add(String name, String type ){ method_decl_args.add(new NameType(name, type)); }

    /* Destroys method_decl args vector */
    public void mdecl_args_destroy(){ method_decl_args = null; }



    //////////////////////
    /* Method Call Args */
    //////////////////////

    /* Adds a new level, for inner method call */
    public void mcall_new_method(){ method_call_args.add(new Vector<>()); }

    /* Adds a new argument in the innermost method call */
    public void mcall_ins_arg(String par){
        int v_size = method_call_args.size();
        if(v_size != 0){
            (method_call_args.get(v_size - 1)).add(par);
        }
        else
            System.out.println("GRANDEEE ERRRRORR IN MCALL_INS");    
    }

    /* Pops the arguments of the innermost method call */
    public Vector<String> mcall_pop_last(){ 
        int v_size = method_call_args.size();
        if(v_size != 0)
            return method_call_args.remove(v_size - 1);
        else
            System.out.println("GRANDEEE ERRRRORR IN MCALL_POP");
        return null;
    }


    //////////////////////
    /* Method Call Regs */
    //////////////////////

    /* Adds a new level, for inner method call registers */
    public void mregs_new_method(){ method_call_regs.add(new Vector<>()); }

    /* Adds a new register in the innermost method call */
    public void mregs_ins_arg(String reg_name, String type){
        int v_size = method_call_regs.size();
        if(v_size != 0){
            (method_call_regs.get(v_size - 1)).add(new NameType(reg_name, type));
        }
        else
            System.out.println("GRANDEEE ERRRRORR IN MREGS_INS");    
    }

    /* Pops the arguments of the innermost method call */
    public Vector<NameType> mregs_pop_last(){ 
        int v_size = method_call_regs.size();
        if(v_size != 0)
            return method_call_regs.remove(v_size - 1);
        else
            System.out.println("GRANDEEE ERRRRORR IN MREGS_POP");
        return null;
    }
}