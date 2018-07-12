package helpers;
import java.util.Arrays;
import java.util.regex.Pattern;

public class FileStorageServer {
	public static void printBuffer(byte[] buffer, String entity) {
		System.out.print(entity + " prints contents of the UDP buffer:");
		System.out.println(Arrays.toString(buffer));
		System.out.print(entity + " prints contents of UDP buffer as string: ");
		System.out.println(new String(buffer));
		System.out.println();
	
}
	public static String bufferToString(byte[] buffer) {
		StringBuilder strBuilder = new StringBuilder();
		for(int i = 0; i< buffer.length; i++) {
			char value = (char)buffer[i];
			if (Character.isLetter(value)) {
				strBuilder.append(value);
			}
			else if (Character.isDigit(value)) {
				strBuilder.append(value);
			}
			else {
				strBuilder.append(buffer[i]);
			}
		}
		return strBuilder.toString();
	
	}
