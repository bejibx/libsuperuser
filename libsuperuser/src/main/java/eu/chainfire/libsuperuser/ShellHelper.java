package eu.chainfire.libsuperuser;


import java.util.List;

public class ShellHelper
{
    public static boolean isBusyboxInstalled()
    {
        List<String> result = Shell.SU.run("busybox");
        return result != null;
    }

    public static void write(String toFile, byte[] bytes, int offset)
    {
        StringBuilder asString = new StringBuilder(bytes.length * 4);
        for (byte current : bytes)
            asString.append(String.format("\\x%02X", current));
        String format = "busybox printf '%1$s' | busybox dd of='%2$s' bs=1 seek=%3$d count=%4$d conv=notrunc";
        String command = String.format(format, asString.toString(), toFile, offset, bytes.length);
        Shell.SU.run(command);
    }

    public static void write(String toFile, byte b, int offset)
    {
        write(toFile, new byte[]{b}, offset);
    }

    public static byte[] read(String fromFile, int offset, int count)
    {
        String format = "busybox od --address-radix=n --output-duplicates --format=x1 --skip-bytes=%1$d --read-bytes=%2$d '%3$s'";
        String command = String.format(format, offset, count, fromFile);
        List<String> result = Shell.SU.run(command);
        byte[] bytes = new byte[count];
        int parsed = 0;
        for (String s : result)
        {
            int len = s.length();
            for (int j = 1; j < len; j += 3, parsed++)
            {
                bytes[parsed] = (byte) ((Character.digit(s.charAt(j), 16) << 4) +
                        Character.digit(s.charAt(j + 1), 16));
            }
        }
        return bytes;
    }

    public static boolean makeDirectory(String path)
    {
        String command = String.format("busybox mkdir %s", path);
        Shell.SU.run(command);
        return isDirectoryExists(path);
    }

    public static boolean isDirectoryExists(String path)
    {
        String command = String.format("[ -d %s ] || busybox echo \"FALSE\"", path);
        List<String> output = Shell.SU.run(command);
        return !(output != null && output.size() > 0) || !output.get(0).contains("FALSE");
    }

    public static boolean createHardLink(String toPath, String wherePath)
    {
        String command = String.format("busybox ln %s %s", toPath, wherePath);
        List<String> output = Shell.SU.run(command);
        return output != null;
    }

    public static String getPermissions(String path)
    {
        String command = "busybox stat -c %a " + path;
        List<String> output = Shell.SU.run(command);
        if (output != null && output.size() > 0)
        {
            return output.get(0).replace("\n", "");
        }
        else
        {
            return "";
        }
    }

    public static String getOwnerAndGroup(String path)
    {
        String command = "busybox stat -c %u.%g " + path;
        List<String> output = Shell.SU.run(command);
        if (output != null && output.size() > 0)
        {
            return output.get(0).replace("\n", "");
        }
        else
        {
            return "";
        }
    }

    public static void setPermissions(String path, String permissions)
    {
        String decimalForm = "";

        if (permissions.length() == 0)
        {
            decimalForm = "0";
        }
        else if (permissions.matches("[0-7]{1,3}"))
        {
            decimalForm = permissions;
        }
        else
        {
            if (permissions.matches("([r-]{1}[w-]{1}[x-]{1}){3}"))
            {
                permissions = permissions.replace('-', '0').replaceAll("[rwx]", "1");
            }
            if (permissions.matches("[0,1]{9}"))
            {
                for (int i = 0; i < 3; i++)
                {
                    decimalForm +=
                            String.valueOf(
                                    Integer.parseInt(
                                            permissions.substring(i * 3, (i * 3) + 3),
                                            2
                                    )
                            );
                }
            }
            else
            {
                String errorMessage = "Permissions \"" + permissions + "\" has invalid format. " +
                        "Valid formats are \"ddd\", \"dd\", \"d\", \"bbbbbbbbb\" and \"rwxrwxrwx\"" +
                        ", where \"d\" is decimal number from 0 to 7 and \"b\" is binary number.";
                throw new IllegalArgumentException(errorMessage);
            }
        }
        String command = String.format("busybox chmod %s %s", decimalForm, path);
        Shell.SU.run(command);
    }

    public static void removeFile(String path)
    {
        String command = "busybox rm " + path;
        Shell.SU.run(command);
    }

    public static boolean copyFile(String filePath, String destPath)
    {
        String command = String.format("busybox cp -f %s %s", filePath, destPath);
        List<String> output = Shell.SU.run(command);
        return output != null;
    }

    public static boolean moveFile(String filePath, String destPath)
    {
        String command = String.format("busybox mv %s %s", filePath, destPath);
        List<String> output = Shell.SU.run(command);
        return output != null;
    }

    public static void setOwnerAndGroup(String path, String ownerAndGroupId)
    {
        String command = String.format("busybox chown %s %s", ownerAndGroupId, path);
        Shell.SU.run(command);
    }

    public static void removeDirectory(String path)
    {
        String command = "busybox rm -r " + path;
        Shell.SU.run(command);
    }

    public static void reboot()
    {
        Shell.SU.run("reboot");
    }

    public static int getFileSize(String path)
    {
        List<String> output = Shell.SU.run("busybox stat -c %s " + path);
        return Integer.decode(output.get(0).replace("\n", ""));
    }
}
