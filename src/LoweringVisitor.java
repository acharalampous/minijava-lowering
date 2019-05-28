/*******************************/
/* LoweringVisitor.java */

/* Name:    Andreas Charalampous
 * A.M :    1115201500195
 * e-mail:  sdi1500195@di.uoa.gr
 */
/********************************/
import java.io.BufferedWriter;
import java.io.IOException;

import syntaxtree.*;
import visitor.GJDepthFirst;


/*
 * Visitor that will be used for lowering to LLVM IR. It uses the offsets collected previously for creating the IR.
 */
public class LoweringVisitor extends GJDepthFirst<String, String>{
    
   private LoweringST symbol_table;
   private BufferedWriter output_file;
   private String cur_class;

   public LoweringVisitor(LoweringST st, BufferedWriter output_file){
      this.symbol_table = st;
      this.output_file = output_file;
   }

   public void emit(String output) throws IOException{
      this.output_file.write(output);
   }

   /* Prepares a register for the var given. Var can be a local variable, class field or literal */
   public String load_variable(String var, String type) throws IOException{
      String reg;
      if(var.substring(0, 1).equals("%")){ // local register
         if(type.equals("int[]")) 
            // emit("\n\t" + reg + " = load i32*, i32** " + var);
            return var;
         reg = symbol_table.get_register();
         if(type.equals("int")) 
            emit("\n\t" + reg + " = load i32, i32* " + var);
         else if(type.equals("boolean")) 
            emit("\n\t" + reg + " = load i8, i8* " + var);
         else 
            emit("\n\t" + reg + " = load i8*, i8** " + var);

      }
      else if(var.length() > 1 && var.substring(0, 2).equals("!!")){ // field of object, must fetch it from memory
         /* Get variable name and its offset to fetch it from memory */
         String var_name = var.substring(2); 
         int var_offset = symbol_table.get_var_offset(cur_class, var_name);

         /* Get ptr to variable and store it to register */
         String tmp_reg1 = symbol_table.get_register();
         emit("\n\t" + tmp_reg1 + " = getelementpr i8, i8* %this, i32 " + var_offset);
         String tmp_reg2 = symbol_table.get_register();
         emit("\n\t" + tmp_reg2 + " = bitcast i8* " + tmp_reg1 + " to i32*");
         reg = symbol_table.get_register();
         emit("\n\t" + reg + " = load i32, i32* " + tmp_reg2);
      }
      else // literal provided   
         reg = var;

      return reg;
   }


	/**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
   public String visit(Goal n, String argu) throws Exception {
      symbol_table.print_vtables(output_file);
      symbol_table.print_ext_methods(output_file);
      String _ret = null;
      n.f0.accept(this, argu);
      //n.f1.accept(this, argu);
      return _ret;
   }
  
   /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
   public String visit(MainClass n, String argu) throws Exception {
      emit("\n\n");
      emit("define i32 @main(){");
      symbol_table.enter_scope();
      n.f14.accept(this, argu);
      n.f15.accept(this, argu);
      n.f16.accept(this, argu);
      n.f17.accept(this, argu);
      emit("\n\n\tret i32 0\n}");
      symbol_table.exit_scope();
      return null;
   }
  
     /**
      * f0 -> ClassDeclaration()
      *       | ClassExtendsDeclaration()
      */
     public String visit(TypeDeclaration n, String argu) throws Exception {
        return n.f0.accept(this, argu);
     }
  
     /**
      * f0 -> "class"
      * f1 -> Identifier()
      * f2 -> "{"
      * f3 -> ( VarDeclaration() )*
      * f4 -> ( MethodDeclaration() )*
      * f5 -> "}"
      */
     public String visit(ClassDeclaration n, String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        return _ret;
     }
  
     /**
      * f0 -> "class"
      * f1 -> Identifier()
      * f2 -> "extends"
      * f3 -> Identifier()
      * f4 -> "{"
      * f5 -> ( VarDeclaration() )*
      * f6 -> ( MethodDeclaration() )*
      * f7 -> "}"
      */
     public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        n.f7.accept(this, argu);
        return _ret;
     }
  
   /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
   public String visit(VarDeclaration n, String argu) throws Exception {
      String type = n.f0.accept(this, argu);
      String id = n.f1.accept(this, argu);
      String llvm_type = symbol_table.get_llvm_type(type);
      emit("\n");
      emit("\t%" + id + " = alloca " + llvm_type);
      emit("\n");

      symbol_table.insert(id, "%" + id, type);
      return null;
   }
  
     /**
      * f0 -> "public"
      * f1 -> Type()
      * f2 -> Identifier()
      * f3 -> "("
      * f4 -> ( FormalParameterList() )?
      * f5 -> ")"
      * f6 -> "{"
      * f7 -> ( VarDeclaration() )*
      * f8 -> ( Statement() )*
      * f9 -> "return"
      * f10 -> Expression()
      * f11 -> ";"
      * f12 -> "}"
      */
     public String visit(MethodDeclaration n, String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        n.f7.accept(this, argu);
        n.f8.accept(this, argu);
        n.f9.accept(this, argu);
        n.f10.accept(this, argu);
        n.f11.accept(this, argu);
        n.f12.accept(this, argu);
        return _ret;
     }
  
     /**
      * f0 -> FormalParameter()
      * f1 -> FormalParameterTail()
      */
     public String visit(FormalParameterList n, String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
     }
  
     /**
      * f0 -> Type()
      * f1 -> Identifier()
      */
     public String visit(FormalParameter n, String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
     }
  
     /**
      * f0 -> ( FormalParameterTerm() )*
      */
     public String visit(FormalParameterTail n, String argu) throws Exception {
        return n.f0.accept(this, argu);
     }
  
     /**
      * f0 -> ","
      * f1 -> FormalParameter()
      */
     public String visit(FormalParameterTerm n, String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
     }
  
     /**
      * f0 -> ArrayType()
      *       | BooleanType()
      *       | IntegerType()
      *       | Identifier()
      */
     public String visit(Type n, String argu) throws Exception {
        return n.f0.accept(this, argu);
     }
  
     /**
      * f0 -> "int"
      * f1 -> "["
      * f2 -> "]"
      */
     public String visit(ArrayType n, String argu) throws Exception {
        return "int[]";
     }
  
     /**
      * f0 -> "boolean"
      */
     public String visit(BooleanType n, String argu) throws Exception {
        return "boolean";
     }
  
     /**
      * f0 -> "int"
      */
     public String visit(IntegerType n, String argu) throws Exception {
        return "int";
     }
  
     /**
      * f0 -> Block()
      *       | AssignmentStatement()
      *       | ArrayAssignmentStatement()
      *       | IfStatement()
      *       | WhileStatement()
      *       | PrintStatement()
      */
     public String visit(Statement n, String argu) throws Exception {
        return n.f0.accept(this, argu);
     }
  
     /**
      * f0 -> "{"
      * f1 -> ( Statement() )*
      * f2 -> "}"
      */
     public String visit(Block n, String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
     }
  
     /**
      * f0 -> Identifier()
      * f1 -> "="
      * f2 -> Expression()
      * f3 -> ";"
      */
     public String visit(AssignmentStatement n, String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        return _ret;
     }
  
     /**
      * f0 -> Identifier()
      * f1 -> "["
      * f2 -> Expression()
      * f3 -> "]"
      * f4 -> "="
      * f5 -> Expression()
      * f6 -> ";"
      */
     public String visit(ArrayAssignmentStatement n, String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        return _ret;
     }
  
     /**
      * f0 -> "if"
      * f1 -> "("
      * f2 -> Expression()
      * f3 -> ")"
      * f4 -> Statement()
      * f5 -> "else"
      * f6 -> Statement()
      */
     public String visit(IfStatement n, String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        return _ret;
     }
  
     /**
      * f0 -> "while"
      * f1 -> "("
      * f2 -> Expression()
      * f3 -> ")"
      * f4 -> Statement()
      */
     public String visit(WhileStatement n, String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        return _ret;
     }
  
     /**
      * f0 -> "System.out.println"
      * f1 -> "("
      * f2 -> Expression()
      * f3 -> ")"
      * f4 -> ";"
      */
     public String visit(PrintStatement n, String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        return _ret;
     }
  
     /**
      * f0 -> AndExpression()
      *       | CompareExpression()
      *       | PlusExpression()
      *       | MinusExpression()
      *       | TimesExpression()
      *       | ArrayLookup()
      *       | ArrayLength()
      *       | MessageSend()
      *       | Clause()
      */
     public String visit(Expression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
     }
  
   /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
   public String visit(AndExpression n, String argu) throws Exception {
      // String n1 = n.f0.accept(this, argu);
      // String reg_n1 = load_variable(n1, "boolean");

      // /* Generate and clause labels */
      // String andlbl1 = symbol_table.get_and_label();
      // String andlbl2 = symbol_table.get_and_label();
      // String andlbl3 = symbol_table.get_and_label();
      // String andlbl4 = symbol_table.get_and_label();

      // emit("\n\tbr label %" + andlbl1);
      // emit("\n\n");
      // emit(andlbl1 + ":");
      // emit("\n\tbr i1 " + reg_n1 + ", label %" + andlbl2 + ", label %" + andlbl4);

      // emit("\n\n");
      // emit(andlbl2 + ":");



      // String n2 =n.f2.accept(this, argu);
      return null;
   }
  
   /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
   public String visit(CompareExpression n, String argu) throws Exception {
      String n1 = n.f0.accept(this, "##");
      String reg_n1 = load_variable(n1, "int");
      
      String n2 = n.f2.accept(this, "##");
      String reg_n2 = load_variable(n2, "int");
      
      String reg_n3 = symbol_table.get_register();
      emit("\n\t" + reg_n3 + " = icmp slt i32 " + reg_n1 + ", " + reg_n2);

      return reg_n3;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
   public String visit(PlusExpression n, String argu) throws Exception {
      String n1 = n.f0.accept(this, "##");
      String reg_n1 = load_variable(n1, "int");
      
      String n2 = n.f2.accept(this, "##");
      String reg_n2 = load_variable(n2, "int");
      
      String reg_n3 = symbol_table.get_register();
      emit("\n\t" + reg_n3 + " = add i32 " + reg_n1 + ", " + reg_n2);

      return reg_n3;
   }
  
   /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
   public String visit(MinusExpression n, String argu) throws Exception {
      String n1 = n.f0.accept(this, "##");
      String reg_n1 = load_variable(n1, "int");
      
      String n2 = n.f2.accept(this, "##");
      String reg_n2 = load_variable(n2, "int");
      
      String reg_n3 = symbol_table.get_register();
      emit("\n\t" + reg_n3 + " = sub i32 " + reg_n1 + ", " + reg_n2);

      return reg_n3;
   }
  
   /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
   public String visit(TimesExpression n, String argu) throws Exception {
      String n1 = n.f0.accept(this, "##");
      String reg_n1 = load_variable(n1, "int");
      
      String n2 = n.f2.accept(this, "##");
      String reg_n2 = load_variable(n2, "int");
      
      String reg_n3 = symbol_table.get_register();
      emit("\n\t" + reg_n3 + " = mul i32 " + reg_n1 + ", " + reg_n2);

      return reg_n3;
   }
  
   /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]" 
    */
   public String visit(ArrayLookup n, String argu) throws Exception {
      /* Get array variable register */
      String reg_n1 = n.f0.accept(this, "##");
      reg_n1 = load_variable(reg_n1, "int[]"); 

      /* Get index register */
      String index = n.f2.accept(this, "##");
      index = load_variable(index, "int");
      
      /* Get array length from first cell */
      String reg_n2 = symbol_table.get_register();
      emit("\n\t" + reg_n2 + " = getelementptr i32, i32* " + reg_n1 + ", i32 0");

      /* Load size to register */
      String reg_n3 = symbol_table.get_register();
      emit("\n\t" + reg_n3 + " = load i32, i32* " + reg_n2);

      /* Perform out of bounds check */
      String reg_n4 = symbol_table.get_register();
      emit("\n\t" + reg_n4 + " = icmp ult i32 " + index + ", " + reg_n3);

      /* Transform ult to "umt" */
      String reg_n5 = symbol_table.get_register();
      emit("\n\t" + reg_n5 + " = xor i1 " + reg_n4 + ", 1");

      /* Branch if-then-else */
      String then_lbl = symbol_table.get_oob_label();
      String else_lbl = symbol_table.get_oob_label();
      emit("\n\tbr i1 " + reg_n5 + ", label %" + then_lbl + ", label %" + else_lbl);
      emit("\n\n");
      
      /* Then */
      emit(then_lbl + ":");
      emit("\n\tcall void @throw_oob()");
      emit("\n\tbr label %" + else_lbl);
      emit("\n\n");
      
      
      /* Else */
      emit(else_lbl + ":");
      
      /* Add + 1 to size, to bypass length cell */
      String reg_n6 = symbol_table.get_register();
      emit("\n\t" + reg_n6 + " = add i32 " + index + ", 1");
      
      /* Get cell to register */
      String reg_n7 = symbol_table.get_register();
      emit("\n\t" + reg_n7 + " = getelementptr i32, i32*  " + reg_n1 + ", i32 " + reg_n6);
      
      return reg_n7;
   }
  
   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
   public String visit(ArrayLength n, String argu) throws Exception {
      /* Get array variable register */
      String n1 = n.f0.accept(this, "##");
      String reg_n1 = load_variable(n1, "int[]");

      /* Get array length from first cell */
      String reg_n2 = symbol_table.get_register();
      emit("\n\t" + reg_n2 + " = getelementptr i32, i32* " + reg_n1 + ", i32 0");
      
      String reg_n3 = symbol_table.get_register();
      emit("\n\t" + reg_n3 + " = load i32, i32* " + reg_n2);

      return reg_n3;
   }

               /**
                  * f0 -> PrimaryExpression()
                  * f1 -> "."
                  * f2 -> Identifier()
                  * f3 -> "("
                  * f4 -> ( ExpressionList() )?
                  * f5 -> ")"
                  */
               public String visit(MessageSend n, String argu) throws Exception {
                  String _ret=null;
                  n.f0.accept(this, argu);
                  n.f1.accept(this, argu);
                  n.f2.accept(this, argu);
                  n.f3.accept(this, argu);
                  n.f4.accept(this, argu);
                  n.f5.accept(this, argu);
                  return _ret;
               }
            
               /**
                  * f0 -> Expression()
                  * f1 -> ExpressionTail()
                  */
               public String visit(ExpressionList n, String argu) throws Exception {
                  String _ret=null;
                  n.f0.accept(this, argu);
                  n.f1.accept(this, argu);
                  return _ret;
               }
            
               /**
                  * f0 -> ( ExpressionTerm() )*
                  */
               public String visit(ExpressionTail n, String argu) throws Exception {
                  return n.f0.accept(this, argu);
               }
            
               /**
                  * f0 -> ","
                  * f1 -> Expression()
                  */
               public String visit(ExpressionTerm n, String argu) throws Exception {
                  String _ret=null;
                  n.f0.accept(this, argu);
                  n.f1.accept(this, argu);
                  return _ret;
               }
  
   /**
    * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
   public String visit(Clause n, String argu) throws Exception {
      return n.f0.accept(this, argu);
   }

   /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | BracketExpression()
    */
   public String visit(PrimaryExpression n, String argu) throws Exception {
      return n.f0.accept(this, argu);
   }
  
   /**
    * f0 -> <INTEGER_LITERAL>
    */
   public String visit(IntegerLiteral n, String argu) throws Exception {
      return n.f0.tokenImage;
   }
  
   /**
    * f0 -> "true"
    */
   public String visit(TrueLiteral n, String argu) throws Exception {
      return "1";
   }
  
   /**
    * f0 -> "false"
    */
   public String visit(FalseLiteral n, String argu) throws Exception {
      return "0";
   }
  
   /**
    * f0 -> <IDENTIFIER>
    */
   public String visit(Identifier n, String argu) throws Exception {
      if(argu != null && argu.equals("##")){ // if var was given, get it's register
         String ret = symbol_table.get_var_reg(n.f0.tokenImage);
         if(ret != null) // register found
            return ret;
         else // not found, return identifier with a sign in order to be used differently
            return "!!" + n.f0.tokenImage;
      }

      return n.f0.tokenImage;
   }

   /**
    * f0 -> "this"
    */
   public String visit(ThisExpression n, String argu) throws Exception {
      return "%this";
   }
  
   /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
   public String visit(ArrayAllocationExpression n, String argu) throws Exception {
      /* Get Expression (Register or Literal) */
      String reg = n.f3.accept(this, "##");
      String reg_n1 = load_variable(reg, "int");
      
      // String reg_n1 = symbol_table.get_register();
      // if(reg.substring(0, 1).equals("%")) // register
      //    emit("\n\t" + reg_n1 + " = load i32, i32* " + reg);
      // else      
      //    reg_n1 = reg;

      
      /* Bounds check */
      String reg_n2 = symbol_table.get_register();
      emit("\n\t" + reg_n2 + "  = icmp slt i32 " + reg_n1 + ", 0");

      /* Branch if-then-else */
      String then_lbl = symbol_table.get_arr_label();
      String else_lbl = symbol_table.get_arr_label();
      emit("\n\tbr i1 " + reg_n2 + ", label %" + then_lbl + ", label %" + else_lbl);
      emit("\n\n");

      /* Then */
      emit(then_lbl + ":");
      emit("\n\tcall void @throw_oob()");
      emit("\n\tbr label %" + else_lbl);
      emit("\n\n");
      
      /* Else */
      emit(else_lbl + ":");

      /* Add + 1 to size, reserve space for array length */
      String reg_n3 = symbol_table.get_register();
      emit("\n\t" + reg_n3 + " = add i32 " + reg_n1 + ", 1");
      
      /* Allocate space for int array */
      String reg_n4 = symbol_table.get_register();
      emit("\n\t" + reg_n4 + " = call i8* @calloc(i32 4, i32 " + reg_n3 + ")");

      /* Store array length at first cell */
      String reg_n5 = symbol_table.get_register();
      emit("\n\t" + reg_n5 + " = bitcast i8* " + reg_n4 + " to i32*");
      emit("\n\tstore i32 " + reg_n1 + ", i32* " + reg_n5); 

      /* Insert register in symbol_table */
      symbol_table.insert(reg_n5, reg_n5, "int[]");
      return reg_n5;
   }
  
   /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
   public String visit(AllocationExpression n, String argu) throws Exception {
      /* Get class name to be created */
      String class_name = n.f1.accept(this, argu);
      ClassOffsets class_info = symbol_table.get_classes().get(class_name);

      /* Get size of object and its vtable */ 
      int size = class_info.get_size();
      int vt_size = class_info.get_vt_size();

      /* Allocate space for object */
      String size_str = Integer.toString(size);
      String reg_n0 = symbol_table.get_register();
      emit("\n\t" + reg_n0 + " = call i8* @calloc(i32 1, i32 " + size_str + ")" );
      
      /* Point to vtable of class */
      String reg_n1 = symbol_table.get_register();
      emit("\n\t" + reg_n1 + " = bitcast i8* " + reg_n0 + " to i8***");
      String reg_n2 = symbol_table.get_register();
      emit("\n\t" + reg_n2 + " = getelementptr[" + vt_size + " x i8*], [" + vt_size + " x i8*]* @." + class_name + "_vtable, i32 0, i32 0");
      emit("\n\tstore i8** " + reg_n2 + ", i8*** " + reg_n1);

      /* Insert register in symbol_table */
      symbol_table.insert(reg_n0, reg_n0, class_name);
      return reg_n0;
   }
  
   /**
    * f0 -> "!"
    * f1 -> Clause()
    */
   public String visit(NotExpression n, String argu) throws Exception {
      /* Get Register/Literal to perform not ~ */
      String var = n.f1.accept(this, argu);
      var = load_variable(var, "boolean");

      /* Perform not expression on value */
      String reg_n1 = symbol_table.get_register();
      emit("\n\t" + reg_n1 + " = xor " + var + ", 1");

      return reg_n1;
   }
  
   /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
   public String visit(BracketExpression n, String argu) throws Exception {
      return n.f1.accept(this, argu);
   }

}