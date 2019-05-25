/*******************************/
/* CheckVisitor.java */

/* Name:    Andreas Charalampous
 * A.M :    1115201500195
 * e-mail:  sdi1500195@di.uoa.gr
 */
/********************************/
import syntaxtree.*;
import visitor.GJDepthFirst;


/*
 * Second of two visitors used in semantic analysis. After collectVisitor collected all classes info,
 * this visitor performs all semantic checks so the program given is correct according to language
 * specifications.
 */
public class CheckVisitor extends GJDepthFirst<String, String>{
    
	private SymbolTable symbol_table;
	private String cur_class; // current class the visitor is in
   private String cur_method; // current method the visitor is in
   private String main_args; // main class arguments variable name, kept so it wont be redeclared in main

   /* Constructor */
   public CheckVisitor(SymbolTable st){
      symbol_table = st;
      cur_class = null;
      cur_method = null;
      main_args = null;
   }


   //
   // User-generated visitor methods below
   //

   /**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
   public String visit(Goal n, String argu) throws Exception {
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
      main_args = n.f11.accept(this, argu); // keep main's args, so they wont be redeclared 
      symbol_table.enter_scope(); // initialize main scope hashmap
      
      n.f14.accept(this, argu);
      n.f15.accept(this, argu);
      
      symbol_table.exit_scope(); // destroy main scope
      main_args = null;
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
      cur_class = n.f1.accept(this, argu); // get class name
      
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
      cur_class =  n.f1.accept(this, argu); // get class name
         
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
		String name = n.f1.accept(this, argu);
      if(main_args != null) // if in main, variable name must not be equal to main class arguments name 
         if(name.equals(main_args))
            throw new Exception("Error during declaration of " + type + " " + name + " in Main method: " + name + " is already declared and used for main's arguments ");

		int result = symbol_table.insert(type, name);
		if(result == -1) // redeclaration
			if(cur_class == null) // in main
				throw new Exception("Error during declaration of " + type + " " + name + " in Main method: " + name + " is already declared");
			else // in class method
				throw new Exception("Error during declaration of " + type + " " + name + " in method " + cur_method + "() of class " + cur_class + ": " + name + " is already declared");
		else if(result == -2) // undefined type
			if(cur_class == null) // in main
				throw new Exception("Error during declaration of " + type + " " + name + " in Main method: Unknown type '" + type + "'" );
			else // in class method
            throw new Exception("Error during declaration of " + type + " " + name + " in method " + cur_method + "() of class " + cur_class + ": Unknown type " + type + "'");


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
      String type = n.f1.accept(this, argu); // return type
      cur_method = n.f2.accept(this, argu); // name of method

      symbol_table.enter_scope(); // initialize scope hashmap
      symbol_table.insert_arguments(cur_class, cur_method); // store arguments in hashmap

      n.f7.accept(this, argu);
      n.f8.accept(this, argu);

      String return_type = n.f10.accept(this, argu);
      if(!return_type.equals(type))
         throw new Exception("Error during declaration of method " + cur_method + "() of class " + cur_class + ": Incompatible return type: '" + return_type + "' cannot be converted to " + type);

      symbol_table.exit_scope(); // destroy current scope
   
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
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
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
      String type1 = n.f0.accept(this, "@ret-type@");
      String type2 = n.f2.accept(this, argu);
      
      if(!symbol_table.is_subtype(type2, type1)){
         if(cur_class == null) // in main
            throw new Exception("Error during assignment operation in Main method : Incompatible Type: '" + type2 + "' cannot be converted to " + type1);
         else // in class method
            throw new Exception("Error during assignment operation in method " + cur_method + "() of class " + cur_class + ": Incompatible Type: '" + type2 + "' cannot be converted to " + type1);
   }
   
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
      String variable_type = n.f0.accept(this, "@ret-type@");
      if(!variable_type.equals("int[]")){ // invalid array type
         if(cur_class == null) // in main
            throw new Exception("Error during array assignment operation in Main method: Incompatible types: '" + variable_type + "' cannot be converted to int[]");
         else // in class method
            throw new Exception("Error during array assignment operation in method " + cur_method + "() of class: Incompatible types: '" + variable_type + "' cannot be converted to int[]");
      }
   
      String index_type = n.f2.accept(this, argu);
      if(!index_type.equals("int")){ // invalid index type
         if(cur_class == null) // in main
            throw new Exception("Error during array assignment operation in Main method: Incompatible types on array index: '" + index_type + "' cannot be converted to int");
         else // in class method
            throw new Exception("Error during array assignment operation in method " + cur_method + "() of class " + cur_class + ": '" + index_type + "' cannot be converted to int");
      }

      String val_type = n.f5.accept(this, argu);
      if(!val_type.equals("int")){ // invalid type
         if(cur_class == null) // in main
            throw new Exception("Error during array assignment operation in Main method: Incompatible Types: " + val_type + " cannot be converted to int.");
         else // in class method
            throw new Exception("Error during array assignment operation in method " + cur_method + "() of class " + cur_class + ": Incompatible Type: " + val_type + " cannot be converted to int.");
      }


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
   String type = n.f2.accept(this, argu);
      if(!type.equals("boolean")){ // Condition of if statement must be of type boolean
         if(cur_class == null) // in main
            throw new Exception("Error during If Statement in Main method : Incompatible type of condition: '" + type + "' cannot be converted to boolean.");
         else // in class method
            throw new Exception("Error during If Statement in method " + cur_method + "() of class " + cur_class + " : Incompatible type of condition: '" + type + "' cannot be converted to boolean.");
      }

      n.f4.accept(this, argu);

      n.f6.accept(this, argu);
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
      String type = n.f2.accept(this, argu);
      if(!type.equals("boolean")){ // Clause of while statement must be of type boolean
         if(cur_class == null) // in main
            throw new Exception("Error during While Statement in Main method: Incompatible type of condition: '" + type + "' cannot be converted to boolean.");
         else // in class method
            throw new Exception("Error during While Statement in method " + cur_method + "() of class " + cur_class + ": Incompatible type of condition: '" + type + "' cannot be converted to boolean.");
		}

		n.f4.accept(this, argu);

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
   String type = n.f2.accept(this, argu);
      if((!type.equals("int")) && (!type.equals("boolean"))){ // println is only valid for int and boolean types
         if(cur_class == null) // in main
            throw new Exception("Error during System.out.println() in Main method: Incompatible types: '" + type + "' cannot be converted to int or boolean");
         else // in class method
            throw new Exception("Error during System.out.println() in method " + cur_method + "() of class " + cur_class + ": Incompatible types: '" + type + "' cannot be converted to int or boolean");
   }
   
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
      String type1 = n.f0.accept(this, argu);
      if(!type1.equals("boolean")){
         if(cur_class == null) // in main
            throw new Exception("Error during logical AND operation(&&) in Main method: '" + type1 + "' cannot be converted to boolean");
         else // in class method
            throw new Exception("Error during logical AND operation(&&) in method " + cur_method + "() of class " + cur_class + ": '" + type1 + "' cannot be converted to boolean");
      }

      String type2 = n.f2.accept(this, argu);
      if(!type2.equals("boolean")){ // * must be applied only on int types
         if(cur_class == null) // in main
            throw new Exception("Error during logical AND operation(&&) in Main method: '" + type2 + "' cannot be converted to boolean");
         else // in class method
            throw new Exception("Error during logical AND operation(&&) in method " + cur_method + "() of class " + cur_class + ": '" + type2 + "' cannot be converted to boolean");         
      }
      return "boolean";
   }
  
   /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
   public String visit(CompareExpression n, String argu) throws Exception {
      String type1 = n.f0.accept(this, argu);
      if(!type1.equals("int")){
         if(cur_class == null) // in main
            throw new Exception("Error during less-than operation(<) in Main method: '" + type1 + "' cannot be converted to int");
         else // in class method
            throw new Exception("Error during less-than operation(<) in method " + cur_method + "() of class " + cur_class + ": '" + type1 + "' cannot be converted to int");
      }

      String type2 = n.f2.accept(this, argu);
      if(!type2.equals("int")){ // * must be applied only on int types
         if(cur_class == null) // in main
            throw new Exception("Error during less-than operation(<) in Main method: '" + type2 + "' cannot be converted to int");
         else // in class method
            throw new Exception("Error during less-than operation(<) in method " + cur_method + "() of class " + cur_class + ": '" + type2 + "' cannot be converted to int");         
      }
      return "boolean";
   }
  
   /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
   public String visit(PlusExpression n, String argu) throws Exception {
      String type1 = n.f0.accept(this, argu);
      if(!type1.equals("int")){
         if(cur_class == null) // in main
            throw new Exception("Error during plus operation(+) in Main method: '" + type1 + "' cannot be converted to int");
         else // in class method
            throw new Exception("Error during plus operation(+) in method " + cur_method + "() of class " + cur_class + ": '" + type1 + "' cannot be converted to int");
      }

      String type2 = n.f2.accept(this, argu);
      if(!type2.equals("int")){ // * must be applied only on int types
         if(cur_class == null) // in main
            throw new Exception("Error during plus operation(+) in Main method: '" + type2 + "' cannot be converted to int");
         else // in class method
            throw new Exception("Error during plus operation(+) in method " + cur_method + "() of class " + cur_class + ": '" + type2 + "' cannot be converted to int");         
      }
      return "int";
   }
  
   /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
   public String visit(MinusExpression n, String argu) throws Exception {
      String type1 = n.f0.accept(this, argu);
      if(!type1.equals("int")){
         if(cur_class == null) // in main
            throw new Exception("Error during minus operation(-) in Main method: '" + type1 + "' cannot be converted to int");
         else // in class method
            throw new Exception("Error during minus operation(-) in method " + cur_method + "() of class " + cur_class + ": '" + type1 + "' cannot be converted to int");
      }

      String type2 = n.f2.accept(this, argu);
      if(!type2.equals("int")){ // * must be applied only on int types
         if(cur_class == null) // in main
            throw new Exception("Error during minus operation(-) in Main method: '" + type2 + "' cannot be converted to int");
         else // in class method
            throw new Exception("Error during minus operation(-) in method " + cur_method + "() of class " + cur_class + ": '" + type2 + "' cannot be converted to int");         
      }
      return "int";
   }
  
   /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
   public String visit(TimesExpression n, String argu) throws Exception {
      String type1 = n.f0.accept(this, argu);
      if(!type1.equals("int")){
         if(cur_class == null) // in main
            throw new Exception("Error during times operation(*) in Main method: '" + type1 + "' cannot be converted to int");
         else // in class method
            throw new Exception("Error during times operation(*) in method " + cur_method + "() of class " + cur_class + ": '" + type1 + "' cannot be converted to int");
      }

      String type2 = n.f2.accept(this, argu);
      if(!type2.equals("int")){ // * must be applied only on int types
         if(cur_class == null) // in main
            throw new Exception("Error during times operation(*) in Main method: '" + type2 + "' cannot be converted to int");
         else // in class method
            throw new Exception("Error during times operation(*) in method " + cur_method + "() of class " + cur_class + ": '" + type2 + "' cannot be converted to int");         
      }
      return "int";
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
   public String visit(ArrayLookup n, String argu) throws Exception {
      String variable_type = n.f0.accept(this, argu);
      if(!variable_type.equals("int[]")){ // invalid array type
         if(cur_class == null) // in main
            throw new Exception("Error during array_indexing operation([]) in Main method: '" + variable_type + "' cannot be converted to int[]");
         else // in class method
            throw new Exception("Error during array_indexing operation([]) in method " + cur_method + "() of class " + cur_class + ": '" + variable_type + "' cannot be converted to int[]");
      }

      String index_type = n.f2.accept(this, argu);
      if(!index_type.equals("int")){ // invalid index type
         if(cur_class == null) // in main
            throw new Exception("Error during array_indexing operation([]) in Main method: Incompatible types on array index: '" + index_type + "' cannot be converted to int");
         else // in class method
            throw new Exception("Error during array_indexing operation([]) in method " + cur_method + "() of class " + cur_class + ": '" + index_type + "' cannot be converted to int");
      }

      return "int";
   }
  
   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
   public String visit(ArrayLength n, String argu) throws Exception {
      String variable_type = n.f0.accept(this, argu);
      if(!variable_type.equals("int[]")){ // invalid array type
         if(cur_class == null) // in main
            throw new Exception("Error during length operation in Main method: '" + variable_type + "' cannot be converted to int[]");
         else // in class method
            throw new Exception("Error during length operation in method " + cur_method + "() of class " + cur_class + ": '" + variable_type + "' cannot be converted to int[]");
      }
      return "int";
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
		String type = n.f0.accept(this, argu);
		String method = n.f2.accept(this, argu);
		if(!symbol_table.is_valid_type(type)){
			if(cur_class == null) // in main
				throw new Exception("Error during method call " + method + "() in Main method: '" + type + "' cannot be dereferenced");
			else // in class method
            throw new Exception("Error during method call " + method + "() in method " + cur_method + "() of class " + cur_class + ": '" + type + "' cannot be dereferenced");
		}
      
      /* Insert new level in methods stack */
		symbol_table.push_back_method();
		n.f4.accept(this, argu);

      /* Check if methods with given args exists in class type */
		String return_type = symbol_table.find_method(type, method);
		if(return_type == null){
			if(cur_class == null) // in main
				throw new Exception("Error during method call " + method + "() in Main method: " + type + " has no method " + method + "() with the given arguments");
			else // in class method
				throw new Exception("Error during method call " + method + "() in method " + cur_method + "() of class " + cur_class + ": " + type + " has no method " + method + "() with the given arguments");
		}

		return return_type;
	}
  
   /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
   public String visit(ExpressionList n, String argu) throws Exception {
      String type = n.f0.accept(this, argu);
      symbol_table.insert_back_arg(type);
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
      String type = n.f1.accept(this, argu);
      symbol_table.insert_back_arg(type);
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
      return n.f0.accept(this, "@ret-type@"); // argument will be used by identifier visitor, to return identifier's type and not name
   }
  
   /**
    * f0 -> <INTEGER_LITERAL>
    */
   public String visit(IntegerLiteral n, String argu) throws Exception {
      return "int";
   }
   
   /**
    * f0 -> "true"
    */
   public String visit(TrueLiteral n, String argu) throws Exception {
      return "boolean";
   }
   
   /**
    * f0 -> "false"
    */
   public String visit(FalseLiteral n, String argu) throws Exception {
      return "boolean";
   }
   
   /**
    * f0 -> <IDENTIFIER>
    */
   public String visit(Identifier n, String argu) throws Exception {
      String name = n.f0.tokenImage;
      if(argu != null)
         if(argu.equals("@ret-type@")){ // Caller wants type of id, must return id type
            String type =  symbol_table.lookup(name, cur_class);
            if(type == null){
               if(cur_class == null) // in main
				      throw new Exception("Error during operation in Main method: Variable '" + name + "' is not declared");
			            else // in class method
                  throw new Exception("Error during operation in method " + cur_method + "() of class " + cur_class + ": Variable '" + name + "' is not declared");
            }

            return type;
		}
		 
		return name;
	}
  
   /**
    * f0 -> "this"
    */
   public String visit(ThisExpression n, String argu) throws Exception {
      if(cur_class == null) // invalid use of this in main
         throw new Exception( "Error in Main method: Invalid use of operator 'this' out of class scope");
      else
         return cur_class; // return type of this
   }
  
   /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
   public String visit(ArrayAllocationExpression n, String argu) throws Exception {
      String type = n.f3.accept(this, null);
      
      if(!type.equals("int")) // index must be int
         if (cur_class == null) // in main
            throw new Exception("Error during new array operation in Main method: Incompatible types on array size: '" + type + "' cannot be converted to int");
         else // in class method
            throw new Exception("Error during new array operation in method " + cur_method + "() of class " + cur_class + ": Incompatible types on array size: '" + type + "' cannot be converted to int");
   
      return "int[]";
   }
  
   /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
   public String visit(AllocationExpression n, String argu) throws Exception {
      String type = n.f1.accept(this, null);
      /* Check if valid type */
      if(!symbol_table.is_valid_type(type))
         if(cur_class == null) // in main
            throw new Exception("Error during new operation in Main method: Unknown type '" + type + "'");
         else // in class method
            throw new Exception("Error during new operation in method " + cur_method + "() of class " + cur_class + ": Unknown type'" + type + "'");
   
      return type;
   }
  
   /**
    * f0 -> "!"
    * f1 -> Clause()
    */
   public String visit(NotExpression n, String argu) throws Exception {
      String type = n.f1.accept(this, argu);
      if(!type.equals("boolean")) // not expression must be applied on boolean type
         if(cur_class == null) // in main
            throw new Exception("Error during logical-not operation(!) in Main method: bad operand type '" + type + "' for unary operator '!'");
         else // in class method
            throw new Exception("Error during logical-not operation(!) in method " + cur_method + "() of class " + cur_class + ": bad operand type " + type + " for unary operator '!'");
   
      return "boolean";
   }
  
   /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
	public String visit(BracketExpression n, String argu) throws Exception {
      return n.f1.accept(this, null);
	}

}