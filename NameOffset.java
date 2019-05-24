/*******************************/
/* NameOffset.java */

/* Name:    Andreas Charalampous
 * A.M :    1115201500195
 * e-mail:  sdi1500195@di.uoa.gr
 */
/********************************/

/* Class that implements the pair of a class' variable/method offset. Holds the name and offset */
public class NameOffset{
    private String name; // name of variable/method
    private int offset; // offset of variable/method

    /* Constructor */
    public NameOffset(String name, int offset){
        this.name = name; 
        this.offset = offset;
    }

    /* Accesors */
    public String get_name(){ return this.name; }
    public int get_offset(){ return this.offset; }
}