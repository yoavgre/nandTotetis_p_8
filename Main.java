

import javax.swing.text.html.parser.Parser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;


public class Main {
    public static void main(String[] args) throws IOException{
           if (args.length == 0) {
               System.err.println("No source file");
                System.exit(1);
          }
        File sourceFile = new File(args[0]);

        //makes a new output file with the correct name.asm according to the directory or file name
        String fileName = sourceFile.getName();
        String filePath = sourceFile.getAbsolutePath();
        String fileNameNoAsm;
        String outDirPath;
        if(!sourceFile.isDirectory()){//single file case - sets the correct path
            fileNameNoAsm = fileName.substring(0, fileName.indexOf(".vm"));
            outDirPath = filePath.substring(0, filePath.indexOf(fileName));
        }
        else {//directory - sets the correct pass
            fileNameNoAsm = fileName;
            outDirPath = filePath+'\\';
        }
        String outputPath = outDirPath + fileNameNoAsm + ".asm";
        File output = new File(outputPath);
        output.createNewFile();

        //creates writer and codeWriter objects
        BufferedWriter outPutWriter = new BufferedWriter(new FileWriter(output)); //writer for writing output
        CodeWriter codeWriter = new CodeWriter(outPutWriter, fileNameNoAsm);

        codeWriter.writeBootsrap();//writes the bootstrap code

        if (sourceFile.isDirectory()) //source is a directory go translate all files in directory
        {
            File[] files = sourceFile.listFiles();
            for (File file : files)
            {
                if(file.getName().indexOf(".vm")!=-1)//vm file
                    translateFile(file,outPutWriter, codeWriter);
            }

        } else //single file
            translateFile(sourceFile,outPutWriter, codeWriter);

        codeWriter.endLines();//write the end loop

        outPutWriter.close();
    }



    /**
     * translate a single vm file
     * @param sourceFile
     * @throws IOException
     */

    public static void translateFile(File sourceFile,BufferedWriter outPutWriter,CodeWriter codeWriter ) throws IOException {
        if (!sourceFile.exists()) {
            System.err.println("file could not be found.");
            System.exit(2);
        }

        Scanner sourceScanner = new Scanner(sourceFile); //scanner for file read
        Parser1 parser = new Parser1 (sourceScanner);

        outPutWriter.write("//new file - "+sourceFile.getName()+"\n");
        codeWriter.setFileName(sourceFile.getName().substring(0,sourceFile.getName().indexOf(".")));//sets the filename with out the .vm
        while(parser.hasMoreLines()) //go over the lines of the vm file
        {
            parser.advance();

            if(parser.commType()== Parser1.commandType.C_ARITHMETIC)
            {
                String arg1 = parser.arg1();
                outPutWriter.write("//"+arg1+"\n");
                codeWriter.writeArithmetic(arg1);
            }
            if(parser.commType()== Parser1.commandType.C_POP || parser.commType()== Parser1.commandType.C_PUSH) {
                String arg1 = parser.arg1();
                int arg2 = parser.arg2();
                outPutWriter.write("//"+parser.commType()+arg1+" "+arg2+"\n");
                codeWriter.writePushPop(parser.commType(), arg1, arg2);
            }
            if(parser.commType()== Parser1.commandType.C_RETURN) {
                outPutWriter.write("//return\n");
                codeWriter.writeReturn();
            }
            if(parser.commType()== Parser1.commandType.C_LABEL) {
                String arg1 = parser.arg1();
                outPutWriter.write("//label "+ arg1+"\n");
                codeWriter.writeLabel(arg1);
            }
            if(parser.commType()== Parser1.commandType.C_GOTO) {
                String arg1 = parser.arg1();
                outPutWriter.write("//goto "+ arg1+"\n");
                codeWriter.writeGoto(arg1);
            }
            if(parser.commType()== Parser1.commandType.C_IF) {
                String arg1 = parser.arg1();
                outPutWriter.write("//gotoIf "+ arg1+"\n");
                codeWriter.writeIf(arg1);
            }
            if(parser.commType()== Parser1.commandType.C_FUNCTION) {
                String arg1 = parser.arg1();
                int arg2 = parser.arg2();
                outPutWriter.write("//function "+arg1+" "+arg2+"\n");
                codeWriter.writeFunction(arg1, arg2);
            }
            if(parser.commType()== Parser1.commandType.C_CALL) {
                String arg1 = parser.arg1();
                int arg2 = parser.arg2();
                outPutWriter.write("//call "+arg1+" "+arg2+"\n");
                codeWriter.writeCall(arg1, arg2);
            }

        }
        sourceScanner.close(); //close the reading from the file
    }
}

