import java.util.Scanner;

public class Parser1 {

    Scanner in;
    String currInst;

    public Parser1(Scanner in)
    {
        this.in = in;
        currInst ="";
    }

    /**
     * @return whether there are more Lines to read
     */
    public boolean hasMoreLines(){
        return in.hasNextLine();
    }

    /**
     * reads the next line, skips empty lines, comment lines
     */
    public void advance()  {
        this.currInst = in.nextLine();
        while((currInst.trim().isEmpty())||(currInst.trim().charAt(0)=='/')) {//skips line with a comment or empty line
            if (in.hasNextLine())
                currInst = in.nextLine();
            else
                break;
        }
        if(currInst.indexOf('/')!=-1) // there is a commant in the instraction
            currInst=currInst.substring(0, currInst.indexOf('/')); // cut the command until the commant
    }

    public enum commandType {C_ARITHMETIC, C_PUSH, C_POP, C_GOTO, C_IF,C_LABEL ,C_CALL, C_FUNCTION, C_RETURN};


    /**
     * returns the command type
     */
    public commandType commType(){
        if(currInst.indexOf("push")!=-1)
            return commandType.C_PUSH;
        if(currInst.indexOf("pop")!=-1)
            return commandType.C_POP;
        if(currInst.indexOf("if")!=-1)
            return commandType.C_IF;
        if(currInst.indexOf("goto")!=-1)
            return commandType.C_GOTO;
        if(currInst.indexOf("label")!=-1)
            return commandType.C_LABEL;
        if(currInst.indexOf("call")!=-1)
            return commandType.C_CALL;
        if(currInst.indexOf("function")!=-1)
            return commandType.C_FUNCTION;
        if(currInst.indexOf("return")!=-1)
            return commandType.C_RETURN;
        else
            return commandType.C_ARITHMETIC;
    }

    /**
     * returns first part of the command
     */

    public String arg1()
    {
        String [] arg = this.currInst.split(" ");
        if(this.commType()==commandType.C_ARITHMETIC)
            return arg[0].trim();
        else
            return arg[1].trim();
    }


    /**
     * returns second part of the command (only in pop or push)
     */
    public int arg2()
    {
        String [] arg = this.currInst.split(" ");
        if(arg[2].indexOf('/')!=-1) //arg 2 is connected with commant need to cut the commant
            arg[2]=arg[2].substring(0, arg[2].indexOf('/'));
        return Integer.parseInt(arg[2].trim());
    }

}
