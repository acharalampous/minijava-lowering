/*******************************/
/* MethodInfo.java */

/* Name:    Andreas Charalampous
 * A.M :    1115201500195
 * e-mail:  sdi1500195@di.uoa.gr
 */
/********************************/
import java.util.*;

/* Class that implements the info (class_name + offset + types) of a method. Will be used for Lowering to LLVM */
public class MethodInfo{
    private String class_name; // name of class that contains method
    private Vector<NameType> types; // return type and arguments of method. Always on 0 its the return type
    private int offset; // offset of method

    /* Constructor */
    public MethodInfo(String class_name, Vector<NameType> types, int offset){
        this.class_name = class_name;
        this.types = types;
        this.offset = offset; 
    }

    /* Accesors */
    public String get_class_name(){ return this.class_name; }
    public Vector<NameType> get_types(){ return this.types; }
    public int get_offset(){ return this.offset; }

    /* Mutators */
    public void set_class_name(String class_name){ this.class_name = class_name; }
    public void set_type(Vector<NameType> types){ this.types = types; }
    public void set_offset(int offset){ this.offset = offset; }

}