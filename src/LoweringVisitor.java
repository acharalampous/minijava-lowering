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
import java.util.*;


/*
 * Visitor that will be used for lowering to LLVM IR. It uses the offsets collected previously for creating the IR.
 */
public class LoweringVisitor extends GJDepthFirst<String, String>{
    
   private LoweringST symbol_table;
   private BufferedWriter output_file;
   private String cur_class;

   private Vector<NameType> method_args;
   private Vector<String> meth_args;

   /* Constructor */
   public LoweringVisitor(LoweringST st, BufferedWriter output_file){
      this.symbol_table = st;
      this.output_file = output_file;
   }

   /* Writes string to output_file */
   public void emit(String output) throws IOException{ this.output_file.write(output); }

   /* Given a string returns true if it is a number, else false */
   public boolean isNumber(String str) { 
      try {  
         Double.parseDouble(str);  
         return true;
      } 
      catch(NumberFormatException e){  
         return false;  
      }  
   }

   /* Converts given var to int, if it of boolean type */
   public String bool_to_int(String var) throws IOException{
      String tmp_reg1;
      String tmp_reg2;
      String var_type;
      if(isNumber(var)){ // literal, does not need a conversion
         return null;
      }
      if(var.substring(0, 1).equals("%")){ // local register
         tmp_reg1 = var;
         var_type = symbol_table.lookup(var); // get type of var
      }
      else{ // identifier, must load its register
         tmp_reg1 = symbol_table.get_var_reg(var); // get local register of identifier         
         if(tmp_reg1 == null){ // object variable
            tmp_reg1 = load_variable(var);
            var_type = symbol_table.lookup(tmp_reg1);
			}
			else
				var_type = symbol_table.lookup(var); // get its type 
         
      }

      if(var_type.equals("i1*")){ // boolean, its value must be loaded
         tmp_reg2 = symbol_table.get_register();
         emit("\n\t" + tmp_reg2 + " = load i1, i1* " + tmp_reg1);
      }
      else if(var_type.equals("i1")){  // boolean, does not need a load 
         tmp_reg2 = tmp_reg1;
      }
      else // not a boolean, does not need any conversion
         return null;
      

      String reg = symbol_table.get_register();
      emit("\n\t" + reg + " = zext i1 " + tmp_reg2 + " to i32");
      
      return reg;
   }

	/* Prepares a register for the var given. Var can be a local variable, class field or literal */
	public String load_variable(String var, String type) throws IOException{
		String tmp_reg;
		String var_type;
		if(isNumber(var)) // Integer/Boolean Literal
				return var;
		else if(var.substring(0, 1).equals("%")){ // local register
			tmp_reg = var;
			var_type = symbol_table.lookup(var); // get type of var
		}
		else{ // identifier, must load its register
			tmp_reg = symbol_table.get_var_reg(var); // get local register of identifier
			
			if(tmp_reg == null){ // object variable
            tmp_reg = load_variable(var);
            var_type = symbol_table.lookup(tmp_reg);
			}
			else
				var_type = symbol_table.lookup(var); // get its type         
		}
         
         
		String reg;
		if(type.equals(var_type)){ // its value must be loaded
			reg = symbol_table.get_register();
			var_type = var_type.substring(0, var_type.length() - 1);
			emit("\n\t" + reg + " = load " + var_type + ", " + type + " " + tmp_reg);
			return reg;
		}
		else
			return var;
   }

   /* Load varibale from object */
   public String load_variable(String var) throws IOException{
      /* Get offset and type */
      int var_offset = symbol_table.get_var_offset(cur_class, var);
      String var_type = symbol_table.get_var_type(cur_class, var);
      var_type = symbol_table.get_llvm_type(var_type);
      String tmp_reg1 = symbol_table.get_register();

      emit("\n\t" + tmp_reg1 + " = getelementptr i8, i8* %this, " + var_type + " " + var_offset);

      String tmp_reg = symbol_table.get_register();
      emit("\n\t" + tmp_reg + " = bitcast i8* " + tmp_reg1 + " to " + var_type + "*");

      var_type = var_type + "*";

      symbol_table.insert(tmp_reg, tmp_reg, var_type);

      return tmp_reg;
   }
   
   /* Loads all method arguments in registers */
   public void load_method_arguments() throws IOException{
      /* Load each argument */
      for(NameType arg : method_args){
         String arg_name = arg.get_name();
         String arg_type = arg.get_type();

         String var_name = arg_name.substring(2); // discard "%."
         emit("\n\t%" + var_name + " = alloca " + arg_type);
         emit("\n\tstore " + arg_type + " " + arg_name + ", " + arg_type + "* %" + var_name);
         emit("\n");

         /* Add arguments to symbol table */
         symbol_table.insert(var_name, "%" + var_name, arg_type + "*");
         if(arg_type.equals("i8*")) // keep argument's class name
            symbol_table.insert_object(var_name, cur_class);
      }

      method_args = null;
   }
	


   
	/**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
   public String visit(Goal n, String argu) throws Exception {
      symbol_table.print_vtables(output_file);
      symbol_table.print_ext_methods(output_file);
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      return null;
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
      cur_class = n.f1.accept(this, argu);
      n.f4.accept(this, argu);
      cur_class = null;
      return null;
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
      cur_class = n.f1.accept(this, argu);
      n.f6.accept(this, argu);
      cur_class = null;
      return null;
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

      symbol_table.insert(id, "%" + id, llvm_type + "*");
      if(llvm_type.equals("i8*"))
         symbol_table.insert_object(id, type);
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
    *  f7 -> ( VarDeclaration() )*
    *  f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
    */
   public String visit(MethodDeclaration n, String argu) throws Exception {
      String ret_type = n.f1.accept(this, argu);
      ret_type = symbol_table.get_llvm_type(ret_type);
      
      String method_name = n.f2.accept(this, argu);
      
      /* Print method's definition */
      emit("\n\ndefine " + ret_type + " @" + cur_class + "." + method_name + "(i8* %this");
      
      /* Print method's arguments */
      method_args = new Vector<>();
      n.f4.accept(this, argu);
      emit("){");
      symbol_table.enter_scope();
      symbol_table.insert_object("%this", cur_class);
      load_method_arguments();

      /* Method Body */
      n.f7.accept(this, argu);
      n.f8.accept(this, argu);
      
      /* Print return statement */
      String ret_val = n.f10.accept(this, argu);
      ret_val = load_variable(ret_val, ret_type + "*");
      emit("\n\n\tret " + ret_type + " " + ret_val);
      emit("\n}");

      symbol_table.exit_scope();

      return null;
   }
  
   /**
    * f0 -> FormalParameter()
    * f1 -> FormalParameterTail()
    */
   public String visit(FormalParameterList n, String argu) throws Exception {
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      return null;
   }

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
   public String visit(FormalParameter n, String argu) throws Exception {
      /* Get type of argument */
      String type = n.f0.accept(this, argu);
      type = symbol_table.get_llvm_type(type);

      /* Get name of argument */
      String arg_name = n.f1.accept(this, argu);

      method_args.add(new NameType("%." + arg_name , type));

      emit(", " + type + " %." + arg_name);
      return null;
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
      n.f1.accept(this, argu);
      return null;
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
      return n.f1.accept(this, argu);
   }
  
   /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
   public String visit(AssignmentStatement n, String argu) throws Exception {
      /* Get variable, it's register and type */
      String id = n.f0.accept(this, null);
      String reg = symbol_table.get_var_reg(id);
      String type;
      if(reg == null){ // object variable
         reg = load_variable(id);
         type = symbol_table.lookup(reg);
      }
      else // local variable
         type = symbol_table.lookup(id);


      /* Load value to be stored */
      String val = n.f2.accept(this, argu);
      val = load_variable(val, type);

      String val_type = type.substring(0, type.length() - 1);

      /* Store value in register */
      emit("\n\tstore " + val_type + " " + val + ", " + type + " " + reg);
      
      return null;
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
      /* Get array variable register */
      String reg_n1 = n.f0.accept(this, null);
      reg_n1 = load_variable(reg_n1, "i32**"); 

      /* Get index register */
      String index = n.f2.accept(this, null);
      index = load_variable(index, "i32*");
      
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
      
      String reg_n8 = n.f5.accept(this, argu);
      reg_n8 = load_variable(reg_n8, "i32*");

      emit("\n\tstore i32 " + reg_n8 + ", i32* " + reg_n7);

      return null;
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
      /* Generate if labels for then_else_continue */
      String then_lbl = symbol_table.get_if_label();
      String else_lbl = symbol_table.get_if_label();
      String con_lbl = symbol_table.get_if_label();

      /* Condition code */
      String reg = n.f2.accept(this, argu);
      reg = load_variable(reg, "i1*"); 


      /* Print condition check */
      emit("\n\tbr i1 " + reg + ", label %" + then_lbl + ", label %" + else_lbl);
      emit("\n\n");
      emit(then_lbl + ":");
      
      /* Then code */
      n.f4.accept(this, argu);      
      
      emit("\n\tbr label %" + con_lbl);
      emit("\n\n");
      emit(else_lbl + ":");
      
      /* Else code */
      n.f6.accept(this, argu);
      emit("\n\tbr label %" + con_lbl);
      emit("\n\n");

      /* Continue after if */
      emit(con_lbl + ":");

      return null;
   }
               
   /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
   public String visit(WhileStatement n, String argu) throws Exception {
      /* Generate loop labels for loop_check-loop_body-after_loop */
      String cond_lbl = symbol_table.get_loop_label();
      String then_lbl = symbol_table.get_loop_label();
      String out_lbl = symbol_table.get_loop_label();

      /* Print loob start */
      emit("\n\tbr label %" + cond_lbl);
      emit("\n\n");
      emit(cond_lbl + ":");

      /* Condition code */
      String reg = n.f2.accept(this, argu);
      reg = load_variable(reg, "i1*");
      emit("\n\tbr i1 " + reg + ", label %" + then_lbl + ", label %" + out_lbl);
      
      /* Loop Body code */
      emit("\n\n");
      emit(then_lbl + ":");
      n.f4.accept(this, argu);
      
      /* Loop Condition recheck code */ 
      emit("\n\tbr label %" + cond_lbl);

      /* Continue after loop */
      emit("\n\n");
      emit(out_lbl + ":");

      return null;
   }
  
   /**
      * f0 -> "System.out.println"
      * f1 -> "("
      * f2 -> Expression()
      * f3 -> ")"
      * f4 -> ";"
      */
   public String visit(PrintStatement n, String argu) throws Exception {
      String val = n.f2.accept(this, null);
      String int_val = bool_to_int(val); // convert to int and load its value if it a boolean 
      if(int_val == null){ // if it was not a boolean, load it's value
         int_val = load_variable(val, "i32*");
      }

      emit("\n\tcall void (i32) @print_int(i32 " + int_val +")");
      return null;
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
      /* Generate and clause labels */
      String andlbl1 = symbol_table.get_and_label();
      String andlbl2 = symbol_table.get_and_label();
      String andlbl3 = symbol_table.get_and_label();
      String andlbl4 = symbol_table.get_and_label();
      
      /* First Clause */
      String n1 = n.f0.accept(this, argu);
      String reg_n1 = load_variable(n1, "i1*");

      emit("\n\tbr label %" + andlbl1);
      emit("\n\n");
      emit(andlbl1 + ":");
      emit("\n\tbr i1 " + reg_n1 + ", label %" + andlbl2 + ", label %" + andlbl4);

      emit("\n\n");
      emit(andlbl2 + ":");

      /* Second Clause */
      String n2 = n.f2.accept(this, argu);
      String reg_n2 = load_variable(n2, "i1*");
      emit("\n\tbr label %" + andlbl3);
      emit("\n\n");
      emit(andlbl3 + ":");
      emit("\n");
      emit("\n\tbr label %" + andlbl4);
      emit("\n\n");
      emit(andlbl4 + ":");

      String reg_n3 = symbol_table.get_register();
      emit("\n\t" + reg_n3 + " = phi i1 [ 0, %" + andlbl1 + " ] , [ " + reg_n2 + ", %" + andlbl3 + " ]");

      symbol_table.insert(reg_n3, reg_n3, "i1");
      
      return reg_n3;
   }
  
   /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
   public String visit(CompareExpression n, String argu) throws Exception {
      String n1 = n.f0.accept(this, null);
      String reg_n1 = load_variable(n1, "i32*");
      
      String n2 = n.f2.accept(this, null);
      String reg_n2 = load_variable(n2, "i32*");
      
      String reg_n3 = symbol_table.get_register();
      emit("\n\t" + reg_n3 + " = icmp slt i32 " + reg_n1 + ", " + reg_n2);

      symbol_table.insert(reg_n3, reg_n3, "i1");
      return reg_n3;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
   public String visit(PlusExpression n, String argu) throws Exception {
      String n1 = n.f0.accept(this, null);
      String reg_n1 = load_variable(n1, "i32*");
      
      String n2 = n.f2.accept(this, null);
      String reg_n2 = load_variable(n2, "i32*");
      
      String reg_n3 = symbol_table.get_register();
      emit("\n\t" + reg_n3 + " = add i32 " + reg_n1 + ", " + reg_n2);

      symbol_table.insert(reg_n3, reg_n3, "i32");
      return reg_n3;
   }
  
   /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
   public String visit(MinusExpression n, String argu) throws Exception {
      String n1 = n.f0.accept(this, null);
      String reg_n1 = load_variable(n1, "i32*");
      
      String n2 = n.f2.accept(this, null);
      String reg_n2 = load_variable(n2, "i32*");
      
      String reg_n3 = symbol_table.get_register();
      emit("\n\t" + reg_n3 + " = sub i32 " + reg_n1 + ", " + reg_n2);

      symbol_table.insert(reg_n3, reg_n3, "i32");
      return reg_n3;
   }
  
   /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
   public String visit(TimesExpression n, String argu) throws Exception {
      String n1 = n.f0.accept(this, null);
      String reg_n1 = load_variable(n1, "i32*");
      
      String n2 = n.f2.accept(this, null);
      String reg_n2 = load_variable(n2, "i32*");
      
      String reg_n3 = symbol_table.get_register();
      emit("\n\t" + reg_n3 + " = mul i32 " + reg_n1 + ", " + reg_n2);

      symbol_table.insert(reg_n3, reg_n3, "i32");

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
      String reg_n1 = n.f0.accept(this, null);
      reg_n1 = load_variable(reg_n1, "i32**"); 

      /* Get index register */
      String index = n.f2.accept(this, null);
      index = load_variable(index, "i32*");
      
      /* Get array length from first cell */
      String reg_n2 = symbol_table.get_register();
      emit("\n\t" + reg_n2 + " = getelementptr i32, i32* " + reg_n1 + ", i32 0");

      /* Load size to register */
      String reg_n3 = symbol_table.get_register();
      emit("\n\t" + reg_n3 + " = load i32, i32* " + reg_n2);

      /* Perform out of bounds check */
      String reg_n4 = symbol_table.get_register();
      emit("\n\t" + reg_n4 + " = icmp ult i32 " + index + ", " + reg_n3);

      /* Transform ult to "ugt" */
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
      
      symbol_table.insert(reg_n7, reg_n7, "i32*");

      return reg_n7;
   }
  
   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
   public String visit(ArrayLength n, String argu) throws Exception {
      /* Get array variable register */
      String n1 = n.f0.accept(this, null);
      String reg_n1 = load_variable(n1, "i32**");

      /* Get array length from first cell */
      String reg_n2 = symbol_table.get_register();
      emit("\n\t" + reg_n2 + " = getelementptr i32, i32* " + reg_n1 + ", i32 0");
      
      String reg_n3 = symbol_table.get_register();
      emit("\n\t" + reg_n3 + " = load i32, i32* " + reg_n2);

      symbol_table.insert(reg_n3, reg_n3, "i32");

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
                  /* Get variable, its register and type */
                  String id = n.f0.accept(this, null);
                  String reg_n1 = load_variable(id, "i8**");

                  String reg_n2 = symbol_table.get_register();
                  emit("\n\t" + reg_n2 + " = bitcast i8* " + reg_n1 + " to i8***");

                  String reg_n3 = symbol_table.get_register();
                  emit("\n\t" + reg_n3 + " = load i8**, i8*** " + reg_n2);
                  
                  String type = symbol_table.get_object_type(id);
                  
                  String method_name = n.f2.accept(this, argu);
                  String offset = Integer.toString(symbol_table.get_method_offset(type, method_name));
                  
                  String reg_n4 = symbol_table.get_register();
                  emit("\n\t" + reg_n4 + " = getelementptr i8*, i8** " + reg_n3 + ", i32 " + offset);

                  String reg_n5 = symbol_table.get_register();
                  emit("\n\t" + reg_n5 + " = load i8*, i8** " + reg_n4);

                  Vector<NameType> method_types = symbol_table.get_classes().get(type).get_methods().get(method_name).get_types();
                  String rtype = method_types.get(0).get_type();
                  String ret_type = symbol_table.get_llvm_type(rtype);

                  String reg_n6 = symbol_table.get_register();
                  emit("\n\t" + reg_n6 + " = bitcast i8* " + reg_n5 + " to " + ret_type + " (i8*");

                  meth_args = new Vector<>();
                  for(int i = 1; i < method_types.size(); i++){
                     String arg_type = symbol_table.get_llvm_type(method_types.elementAt(i).get_type());
                     emit(", " + arg_type);

                     meth_args.add(arg_type);
                  }
                  
                  emit(")*");

                  method_args = new Vector<>();
                  n.f4.accept(this, argu);

                  String reg_n7 = symbol_table.get_register();
                  emit("\n\t" + reg_n7 + " = call " + ret_type + " " + reg_n6 + "(i8* " + reg_n1);
                  for(int i = 0; i < method_args.size(); i++){
                     NameType argument = method_args.elementAt(i);
                     String arg_reg = argument.get_name();
                     String arg_type = argument.get_type();

                     emit(", " + arg_type + " " + arg_reg);
                  }

                  emit(")");

                  
                  symbol_table.insert(reg_n7, reg_n7, ret_type);
                  if(ret_type.equals("i8*")){
                     symbol_table.insert_object(reg_n7, rtype);
                  }
                  return reg_n7;
               }
            
               /**
                  * f0 -> Expression()
                  * f1 -> ExpressionTail()
                  */
               public String visit(ExpressionList n, String argu) throws Exception {
                  String arg = n.f0.accept(this, argu);
                  String type = meth_args.remove(0);
                  String arg_reg = load_variable(arg, type + "*");

                  method_args.add(new NameType(arg_reg, type));

                  n.f1.accept(this, argu);
                  
                  return null;
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
                  String arg = n.f1.accept(this, argu);
                  String type = meth_args.remove(0);
                  String arg_reg = load_variable(arg, type + "*");

                  method_args.add(new NameType(arg_reg, type));

                  return null;
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
      String reg = n.f3.accept(this, null);
      String reg_n1 = load_variable(reg, "i32*");
      
      /* Bounds check */
      String reg_n2 = symbol_table.get_register();
      emit("\n\t" + reg_n2 + "  = icmp slt i32 " + reg_n1 + ", 1");

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
      symbol_table.insert(reg_n5, reg_n5, "i32*");
      
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
      symbol_table.insert(reg_n0, reg_n0, "i8*");
      symbol_table.insert_object(reg_n0, class_name);
      return reg_n0;
   }
  
   /**
    * f0 -> "!"
    * f1 -> Clause()
    */
   public String visit(NotExpression n, String argu) throws Exception {
      /* Get Register/Literal to perform not ~ */
      String var = n.f1.accept(this, argu);
      var = load_variable(var, "i1*");

      /* Perform not expression on value */
      String reg_n1 = symbol_table.get_register();
      emit("\n\t" + reg_n1 + " = xor i1" + var + ", 1");

      symbol_table.insert(reg_n1, reg_n1, "i1");

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