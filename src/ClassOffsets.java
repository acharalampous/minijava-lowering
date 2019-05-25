/*******************************/
/* ClassOffsets.java */

/* Name:    Andreas Charalampous
 * A.M :    1115201500195
 * e-mail:  sdi1500195@di.uoa.gr
 */
/********************************/
import java.util.*;

/* Class that implements all the necessary info required about a class, to be used in LLVM Lowering */
public class ClassOffsets{
    private int size; // size of class (Vtable ptr[8] + size of all variables)
    private int vtable_size; // size of vtable(# of all methods)
    private Map<String, VariableInfo> variables; // maps variable's name with its info(type + offset)
    private Map<String, MethodInfo> methods; // maps method's name with its info(class_names + types + offset)

    /* Constructor */
    public ClassOffsets(){
        this.size = 8;
        this.vtable_size = 0;
        this.variables = new LinkedHashMap<>();
        this.methods = new LinkedHashMap<>();
    }

    public void fill_offsets(String class_name, ClassContent cc){
        /* Get all variables of class */
        Vector<NameOffset> v_offsets = cc.get_v_offsets();
        Map<String, String> variables = cc.get_variables();
        for(NameOffset var : v_offsets){
            String v_name = var.get_name();
            String v_type = variables.get(v_name);
            int v_offset = var.get_offset() + 8;

            this.variables.put(v_name, new VariableInfo(v_type, v_offset));

            int v_size = 0;
            if(v_type.equals("boolean")) v_size = 1;
            else if(v_type.equals("int")) v_size = 4;
            else v_size = 8;

            increase_size(v_size);
        }

        Vector<NameOffset> m_offsets = cc.get_m_offsets();
        Map<String, Vector<NameType>> methods = cc.get_methods();
        for(NameOffset meth : m_offsets){
            String m_name = meth.get_name();
            if(this.methods.containsKey(m_name)){
                this.methods.get(m_name).set_class_name(class_name);
            }
            else{
                Vector<NameType> m_args = methods.get(m_name);
                int m_offset = this.vtable_size;
    
                this.methods.put(m_name, new MethodInfo(class_name, m_args, m_offset));

                increase_vt_size(1);
            }
        }

        for(Map.Entry<String, Vector<NameType>> entry : cc.get_methods().entrySet()){
            String m_name = entry.getKey();
            if(this.methods.containsKey(m_name)){
                this.methods.get(m_name).set_class_name(class_name);
            }
        }
        
    }

    /* Accesors */
    public int get_size(){ return this.size; }
    public int get_vt_size(){ return this.vtable_size; }
    public Map<String, VariableInfo> get_variables(){ return this.variables; }
    public Map<String, MethodInfo> get_methods(){ return this.methods; }

    /* Mutators */
    public void set_size(int size){ this.size = size; }
    public void increase_size(int size){ this.size += size; }

    public void set_vt_size(int vt_size){ this.vtable_size = vt_size; }
    public void increase_vt_size(int vt_size){ this.vtable_size += vt_size; }

    public void print_all(){
        System.out.println("\tSize: " + this.size);
        System.out.println("\tVTable Size: " + this.vtable_size);        
        System.out.println("\tVariables: ");
        for(Map.Entry<String, VariableInfo> entry : this.variables.entrySet()){
            String v_name = entry.getKey();
            System.out.println("\t\t" + v_name + " | " + entry.getValue().get_type() + " | " + entry.getValue().get_offset());
        }

        System.out.println("\tMethods: ");
        for(Map.Entry<String, MethodInfo> entry : this.methods.entrySet()){
            String m_name = entry.getKey();
            System.out.println("\t\t" + m_name + " | " + entry.getValue().get_class_name() + " | " + entry.getValue().get_offset());
        }
    }
}