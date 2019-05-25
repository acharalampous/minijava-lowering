/*******************************/
/* LoweringST.java */

/* Name:    Andreas Charalampous
 * A.M :    1115201500195
 * e-mail:  sdi1500195@di.uoa.gr
 */
/********************************/
import java.util.*;


/* Implementation of the Symbol Table that it will be used for Lowering to LLVM from MiniJava */
public class LoweringST{
    private Map<String, ClassOffsets> classes; // maps class names with their offset info

    private Map<String, String> current_scope; // Holds all the declared variables with their type, in current scope

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

    public void print_all(){
        for (Map.Entry<String, ClassOffsets> entry : this.classes.entrySet()){
            System.out.println("\n\n" + entry.getKey());
            entry.getValue().print_all();
        }
    }
}