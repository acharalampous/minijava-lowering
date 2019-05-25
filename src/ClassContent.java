/*******************************/
/* ClassContent.java */

/* Name:    Andreas Charalampous
 * A.M :    1115201500195
 * e-mail:  sdi1500195@di.uoa.gr
 */
/********************************/
import java.util.*;

/* Keeps all the content of a class, like its and superclass variables/methods and their offset */ 
public class ClassContent{
    Map<String, String> variables; // Maps the variables' name of a Class with it's type
    Map<String, String> s_variables; // Like variables, but holds variables of superclasses

    Map<String, Vector<NameType>> methods; // Maps the method's name with its return type and arguments. Always on 0 its the return type
    Map<String, Vector<NameType>> s_methods; // Like methods, but holds methods of superclasses    
    
    Vector<NameOffset> v_offsets; // Vector that holds Pairs of <variable, offset>
    Vector<NameOffset> m_offsets; // Vector that holds Pairs of <method, offset>

    int v_next_offset; // Offset of next variable
    int m_next_offset; // Offset of next method

    /* Constructor */
    public ClassContent(){
        variables = new HashMap<>();
        s_variables = new HashMap<>();

        methods = new HashMap<>();
        s_methods = new HashMap<>();

        v_offsets = new Vector<>();
        m_offsets = new Vector<>();

        v_next_offset = 0;
        m_next_offset = 0;

    }

    
    /* Given a new variable's type and name, store it's offset and increase next offset for variables */
    public void add_v_offset(String type, String name){
        /* Add new variable and offset */
        v_offsets.add(new NameOffset(name, this.v_next_offset));
        
        /* Compute new next offset */
        int offset = 0;
        if(type.equals("int"))
            offset = 4;
        else if(type.equals("boolean"))
            offset = 1;
        else
            offset = 8;

        /* Set next offset */
        this.v_next_offset += offset;
    }
    
    /* Given a new method, store it's offset and increase next offset for methods */
    public void add_m_offset(String name){
        m_offsets.add(new NameOffset(name, this.m_next_offset));
        
        this.m_next_offset += 8; // increase next offset with a size of pointer
    }
    
    
    
    /* Mutators */
    public void set_v_offset(int off){ this.v_next_offset = off; }
    public void set_m_offset(int off){ this.m_next_offset = off; }

    public void add_method(String name, Vector<NameType> parameters){ methods.put(name, parameters); }
    public void add_s_method(String name, Vector<NameType> parameters){ s_methods.put(name, parameters); }
    /* Store super's class variables/methods in class contents */
    public void inherite_variables(Map<String, String> s_variables){ this.s_variables.putAll(s_variables); }
    public void inherite_methods(Map<String, Vector<NameType>> s_methods){ this.s_methods.putAll(s_methods); }
    

    /* Accesors */
    public Map<String, String> get_variables(){ return variables; }
    public Map<String, String> get_s_variables(){ return s_variables; }

    public Map<String, Vector<NameType>> get_methods(){ return methods; }
    public Map<String, Vector<NameType>> get_s_methods(){ return s_methods; }

    public int get_v_next_offset(){ return v_next_offset; }
    public int get_m_next_offset(){ return m_next_offset; }

    public Vector<NameOffset> get_v_offsets(){ return v_offsets; }
    public Vector<NameOffset> get_m_offsets(){ return m_offsets; }
    
}