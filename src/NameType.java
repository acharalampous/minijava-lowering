/*******************************/
/* NameType.java */

/* Name:    Andreas Charalampous
 * A.M :    1115201500195
 * e-mail:  sdi1500195@di.uoa.gr
 */
/********************************/

/* Class that implements the pair of (a method's argument) name and type <name, type>. Holds the name and type */
public class NameType{
    private String name; // name of argument
    private String type; // type of argument

    /* Constructor */
    public NameType(String name, String type){
        this.name = name; 
        this.type = type;
    }

    /* Accesors */
    public String get_name(){ return this.name; }
    public String get_type(){ return this.type; }
}