import javax.imageio.IIOException;
import java.io.BufferedWriter;
import java.io.IOException;


public class CodeWriter {

    private BufferedWriter output;
    String fileName; //saves the file name for static memory
    private static int ifIndex = 1;//index for if conditions in eq, gt,lt commands
    private static int returnIndex = 1; //index for return labels
    private final int TEMP_INDEX=5;
    private final String ADDR= "addr";
    private final String SP= "SP";


    public CodeWriter(BufferedWriter writer, String fileName){
        this.output=writer;
        this.fileName = fileName; //saves the fileName without the extension
    }

    /**
     * writes the bootstrap call
     * @throws IOException
     */
    public void writeBootsrap()throws IOException{
        output.write("@256\n"+
                        "D=A\n" +
                        "@SP\n"+
                        "M=D\n");
        writeCall("Sys.init", 0);
    }


    /**
     * writes arithmetic command
     * @param command
     * @throws IOException
     */
    public void writeArithmetic (String command) throws IOException{

        spMinus();
        if(command.equals("add"))
        {
            D_sp();
            spMinus();
            output.write("A=M\n");
            output.write("M=D+M\n");
        }

        if(command.equals("sub"))
        {
            D_sp(); // D=RAM[SP]
            spMinus();
            output.write("A=M\n");
            output.write("M=M-D\n");
        }
        if(command.equals("neg"))
        {
            output.write("@"+SP+"\n");
            output.write("A=M\n");
            output.write("M=-M\n");
        }
        if(command.equals("eq")) //using the ifIndex to create diffrent variable for every condition
        {
            /*
              D=RAM[sp]
              sp--
              @Sp
             * A=M
             * D=D-M
             * @true1
             * D:JEQ // d==0
             * @sp
             * A=M
             * M=0
             * @false1
             * 0;jmp
             * (true1)
             * @sp
             * A=M
             * M=-1
             * (false1)
             */
            D_sp(); // D=RAM[sp]
            spMinus();
            output.write("@SP\n");
            output.write("A=M\n");
            output.write("D=D-M\n");
            output.write("@" + "TRUE"+ifIndex+"\n");
            output.write("D;JEQ\n");
            ifLines();
            ifIndex++;
        }
        if(command.equals("gt"))
        {
            D_sp(); // D=RAM[sp]
            spMinus();
            output.write("@SP\n");
            output.write("A=M\n");
            output.write("D=D-M\n");
            output.write("@" + "TRUE"+ifIndex+"\n");
            output.write("D;JLT\n");
            ifLines();
            ifIndex++;
        }
        if(command.equals("lt"))
        {
            D_sp(); // D=RAM[sp]
            spMinus();
            output.write("@SP\n");
            output.write("A=M\n");
            output.write("D=D-M\n");
            output.write("@" + "TRUE"+ifIndex+"\n");
            output.write("D;JGT\n");
            ifLines();
            ifIndex++;
        }
        if(command.equals("and"))
        {
            D_sp();
            spMinus();
            output.write("A=M\n");
            output.write("M=D&M\n");
        }
        if(command.equals("or"))
        {
            D_sp(); // D=RAM[SP]
            spMinus();
            output.write("A=M\n");
            output.write("M=D|M\n");
        }
        if(command.equals("not"))
        {
            output.write("@"+SP+"\n");
            output.write("A=M\n");
            output.write("M=!M\n");
        }
        spPlus(); //in all cases puts the pointer in the end of the stack

    }


    /**
     * writes the reparative lines for "if" statements
     */
    private void ifLines() throws IOException
    {
        output.write("@" +SP+ "\n"+
                "A=M\n"+
                "M=0\n"+
                "@SKIP"+ifIndex+"\n"+
                "0;JMP\n"+
                "(TRUE"+ifIndex+")\n"+
                "@SP\n"+
                "A=M\n"+
                "M=-1\n"+
                "(SKIP"+ifIndex+")\n");
    }



    /**
     *writes push and pop commands
     * @param command
     * @param arg1
     * @param arg2
     * @throws IOException
     */
    public void writePushPop (Parser1.commandType command, String arg1, int arg2 ) throws IOException{
        boolean segmentArg = arg1.equals("local") || arg1.equals("argument") || arg1.equals("this") || arg1.equals("that");
        if(command == Parser1.commandType.C_PUSH) // handling push argument
        {
            if(arg1.equals("constant")) //handling constant
            {
                ram_sp_eq_num(arg2); //RAM[sp] = arg2

            }
            if(segmentArg) //local or argument or this or that
            {
                addr_arg_i(convertSegment(arg1), arg2); //addr = segmentPointer + i
                ptrToPtr(SP, ADDR);//RAM[RAM[SP]] = RAM[RAM[addr]]
            }
            if(arg1.equals("static"))
            {
                //pointer = index
                // RAM[RAM[SP]] = RAM[filename.i]
                ptrEqIndex(SP, (fileName+"."+arg2));

            }
            if(arg1.equals("temp"))
            {
                // RAM[RAM[SP]] = RAM[5+i]
                ptrEqIndex(SP, Integer.toString(TEMP_INDEX+arg2));

            }
            if(arg1.equals("pointer"))
            {
                if(arg2==0) // push THIS
                {
                    //RAM[SP] = RAM[THIS-not as pointer]
                    ptrEqIndex(SP, "THIS");
                }
                if(arg2==1)
                {
                    //RAM[SP] = RAM[THAT-not as pointer]
                    ptrEqIndex(SP, "THAT");
                }
            }
            spPlus();
        }
        else //pop argument
        {
            if(segmentArg)
            {
                addr_arg_i(convertSegment(arg1), arg2); //addr = segmentPointer + i
                spMinus(); //sp--
                ptrToPtr(ADDR, SP);//RAM[addr] = RAM[sp]
            }

            if(arg1.equals("static"))
            {
                spMinus(); //sp--
                // RAM[filename.i] = RAM[Ram[sp]]
                indexEqPtr((fileName+"."+arg2), SP);

            }
            if(arg1.equals("temp"))
            {
                spMinus();//SP--
                // RAM[5+i] = RAM[sp]
                indexEqPtr(Integer.toString(TEMP_INDEX+arg2), SP);
            }
            if(arg1.equals("pointer"))
            {
                if(arg2==0) // pop THIS
                {
                    spMinus(); //sp--
                    //RAM[THIS] = RAM[sp]
                    indexEqPtr("THIS", SP);

                }
                if(arg2==1) // pop THAT
                {
                    spMinus();//sp--
                    //RAM[THAT] = RAM
                    indexEqPtr("THAT", SP);

                }
            }
        }


    }

    /**
     * helper function - converts segments to their name in memory
     *
     */
    private String convertSegment(String segment)
    {
        switch(segment){
            case "local":
                return "LCL";
            case "argument":
                return "ARG";
            case "this":
                return "THIS";
            case "that":
                return "THAT";
        }
        return null;

    }

    /**
     * writes SP++
     * @throws IOException
     */
    private void spPlus () throws IOException
    {
        output.write("@SP\n");
        output.write("M=M+1\n");
    }


    /**
     * writes SP--
     * @throws IOException
     */
    private void spMinus () throws IOException
    {
        output.write("@SP\n");
        output.write("M=M-1\n");
    }

    // writes lines for D=RAM[sp]
    private void D_sp() throws IOException
    {
        output.write("@"+SP+"\n");
        output.write("A=M\n");
        output.write("D=M\n");
    }

    /**
     * write addr = arg+i
     * @throws IOException
     */
    private void addr_arg_i (String arg, int i) throws IOException
    {
        output.write("@"+i+"\n");
        output.write("D=A"+"\n");
        output.write("@"+arg+"\n");
        output.write("D=D+M\n");
        output.write("@addr\n");
        output.write("M=D\n");
    }


    /**
     * writes RAM[indexTarget] = RAM[RAM[ptrSource]]
     */

    private void indexEqPtr (String indexTarget, String ptrSource)  throws IOException
    {
        output.write("@"+ptrSource+"\n");
        output.write("A=M\n");
        output.write("D=M\n");
        output.write("@"+(indexTarget)+"\n");
        output.write("M=D\n");
    }

    /**
     * writes RAM[RAM[target]] = RAM[INDEX]
     */
    private void ptrEqIndex (String ptrTarget, String indexSource)  throws IOException
    {
        output.write("@"+(indexSource)+"\n");
        output.write("D=M\n");
        output.write("@"+ptrTarget+"\n");
        output.write("A=M\n");
        output.write("M=D\n");
    }

    /**
    writes ram[indexTarget] = ram[indexSource]
     */
    private void indexEqIndex (String indexTarget, String indexSource)  throws IOException
    {
        output.write("@"+(indexSource)+"\n");
        output.write("D=M\n");
        output.write("@"+indexTarget+"\n");
        output.write("M=D\n");
    }

    /**
     writes RAM[RAM[target]] = RAM[RAM[source]]
     */
    private void ptrToPtr (String target, String source) throws IOException
    {
        /*
          RAM[RAM[target]] = RAM[RAM[source]]
          @source
         * A=M
         * D=M
         * @target
         * A=M
         * M=D
         */

        output.write("@"+source+"\n");
        output.write("A=M\n");
        output.write("D=M\n");
        output.write("@"+target+"\n");
        output.write("A=M\n");
        output.write("M=D\n");
    }

    /**
     * writes RAM[Ram[sp]] = num
     */
    private <T> void ram_sp_eq_num(T num) throws IOException
    {
        //D=num
        output.write("@"+num+"\n");
        output.write("D=A\n");
        //Ram[sp] = D
        output.write("@SP\n");
        output.write("A=M\n");
        output.write(("M=D\n"));

    }

    /**
     * writes the end loop to the file
     */
    public void endLines () throws IOException
    {
        output.write("(END)\n"+
                "@END\n"+
                "0;JMP\n");
    }

    /**
     * sets a new file name
     */
    public void setFileName(String name)
    {
        fileName=name;
    }

    /**
     * writes label command
     */
    public void writeLabel(String label) throws IOException
    {
        output.write("("+label+")\n");
    }

    /**
     * writes GO-TO command
     */
    public void writeGoto(String label) throws IOException
    {
        output.write("@"+label+"\n"+
                        "0;JMP\n");
    }

    /**
     * writes IF-GO-TO command
     */
    public void writeIf(String label) throws IOException
    {
        spMinus();
        output.write("@SP\n" +
                "A=M\n" +
                "D=M\n" +
                "@"+label+"\n" +
                "D;JNE\n");
    }

    /**
     * writes function command
     * @param functionName
     * @param nVars number of variables
     */
    public void writeFunction(String functionName, int nVars) throws IOException
    {
        /**
         * (functionName)
         * push 0
         * ....
         * push 0
         */
        writeLabel(functionName);
        for (int i = 0; i<nVars; i++)
        {
            ram_sp_eq_num(0);
            spPlus();
        }
    }

    /**
     * writes call command
     * @param functionName
     * @param nArgs number of arguments (was pushed before the call)
     */
    public void writeCall(String functionName, int nArgs) throws IOException
    {
        String returnLabel = functionName+"$ret."+(returnIndex++);
        ram_sp_eq_num(returnLabel);//push the return label to the stack
        spPlus();
        //push lcl:
        ptrEqIndex(SP, "LCL");
        spPlus();
        //push ARG
        ptrEqIndex(SP, "ARG");
        spPlus();
        //push this
        ptrEqIndex(SP, "THIS");
        spPlus();
        //push that
        ptrEqIndex(SP, "THAT");
        //ARG = sp - 5 - nArgs
        spPlus();
        output.write("@5\n " +
                "D=A\n"+
                "@"+nArgs+"\n"+
                "D=D+A\n"+
                "@SP\n" +
                "D=M-D\n"+
                "@ARG\n"+
                "M=D\n");
        //lcl=sp
        indexEqIndex("LCL", SP);
        //goto functionName
        writeGoto(functionName);

        output.write("("+returnLabel+")\n");
    }

    /**
     * writes return command
     *
     */
    public void writeReturn() throws IOException
    {
        //endframe(temp1) = LCL
        indexEqIndex("endFrame", "LCL");
        //retAddr = *(endframe-5):
        output.write("@5\n");
        output.write("D=A\n");
        output.write("@endFrame"+"\n");
        output.write("D=M-D\n");
        output.write("A=D\n" +
                         "D=M\n" +
                         "@retAddr"+"\n" +
                         "M=D\n");

        //*ARG = pop():
        writePushPop(Parser1.commandType.C_POP, "argument", 0);

        //SP=ARG+1
        output.write("@ARG\n" +
                         "D=M\n" +
                         "D=D+1\n" +
                         "@SP\n" +
                         "M=D\n");

        //THAT=*(endframe-1)
        x_eq_ptr_minus_i("THAT", "endFrame", 1);
        //THIS=*(endframe-2)
        x_eq_ptr_minus_i("THIS", "endFrame", 2);
        //ARG=*(endframe-3)
        x_eq_ptr_minus_i("ARG", "endFrame", 3);
        //LCL=*(endframe-4)
        x_eq_ptr_minus_i("LCL", "endFrame", 4);

        //goto retAddr
        output.write("@retAddr\n");
        output.write("A=M\n"+
                         "0;JMP\n");
    }


    /**
     * helper function : RAM[X] = RAM[RAM[ptr]-i]
     */
    private void x_eq_ptr_minus_i(String x, String ptr, int i) throws IOException
    {
        output.write("@"+i+"\n" +
                         "D=A\n" +
                         "@"+ptr+"\n" +
                         "D=M-D\n" +
                         "A=D\n" +
                         "D=M\n" +
                         "@"+x+"\n" +
                         "M=D\n");
    }



}
