/*******************************/
/* VariableInfo.java */

/* Name:    Andreas Charalampous
 * A.M :    1115201500195
 * e-mail:  sdi1500195@di.uoa.gr
 */
/********************************/

/* Class that implements the pair of type + offset of a variable. Will be used for Lowering to LLVM */
public class VariableInfo{
    private String type; // type of variable
    private int offset; // offset of variable

    /* Constructor */
    public VariableInfo(String type, int offset){
        this.type = type;
        this.offset = offset; 
    }

    /* Accesors */
    public String get_type(){ return this.type; }
    public int get_offset(){ return this.offset; }

    /* Mutators */
    public void set_type(String type){ this.type = type; }
    public void set_offset(int offset){ this.offset = offset; }

}