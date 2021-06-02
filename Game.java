
import java.util.*;
import java.util.Scanner;
//import java.security.*;
import java.security.SecureRandom;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.logging.Logger;

public class Game {

    private User user;
    private Computer computer;
    private Scanner scn;
    private Hashtable<Integer, String> possibleMoves;
    private static final String HMAC_SHA512 = "HmacSHA512";
    private static final Logger LOGGER = Logger.getLogger(Game.class.getName());
    private SecureRandom secureRandom;
    private int possibleMovesSize;
    private int computerMove;
    private int userMove;
    
    public Game () {
        user = new User();
        computer = new Computer();
        possibleMoves = new Hashtable<Integer, String>();
        secureRandom = new SecureRandom();
    }

    public static void main(String[] args) {
        Game Game = new Game ();
        Game.GetListOfMoves(args);
      //  Game.GetSelectedMoves();
    }

    public void GetListOfMoves(String[] args) {
        if (args.length % 2 == 1) {
            GotOddNumberOfMoves(args);
        } else {
            GetListOfMoves(GotEvenNumberOfMoves());
        }      
    }

    public void GotOddNumberOfMoves(String[] args) {
        System.out.println("*******  Let's have fun! Now possible moves are:  *******\n");
        for (int i = 0, k = 1; i < args.length; i++, k++) {
            possibleMoves.put(k, args[i]);
        }
        possibleMoves.put(0, "Exit");
        for (int j = 1; j < possibleMoves.size(); j++) {
            System.out.println(j + ". " + possibleMoves.get(j));
        }
        possibleMovesSize = possibleMoves.size() - 1;
        
        System.out.println(0 + ". " + possibleMoves.get(0) + "\n");
    }

    public String[] GotEvenNumberOfMoves() {
        System.out.println("The number of arguments must be odd! Try again.\n");
        String userInput = scn.nextLine();
        String[] myArgs = userInput.split(" ");
        return myArgs;
    }

    public class User {

        public User() {
            scn = new Scanner(System.in);
        }

        public Integer GetUserPath(Hashtable<Integer, String> moves) {
            System.out.print("Enter your choice: ");
            String userInput = scn.nextLine();
            return isDigit(userInput) ? GotDigitalInput(userInput, moves) : GotStringInput(userInput, moves);
        }

        public Integer GotDigitalInput(String input, Hashtable<Integer, String> moves) {
            Integer num;
            if (Integer.parseInt(input) < 0 || Integer.parseInt(input) > moves.size() - 1) {
                System.out.println("Incorrect number. Try again.\n");
                num = GetUserPath(moves);
            } else {
                num = Integer.parseInt(input);
            }
            if (num == 0) {
                exitFromApplication();
            }
            return num;
        }

        public Integer GotStringInput(String input, Hashtable<Integer, String> moves) {
            Integer num = -1;
            if (moves.contains(input)) {
                for (Map.Entry entry : moves.entrySet()) {
                    if (input.equals(entry.getValue())) {
                        num = (Integer) entry.getKey();
                        break;
                    }
                }
            } else {
                System.out.println("Incorrect name. Try again.\n");
                num = GetUserPath(moves);
            }
            return num;
        }

        public boolean isDigit(String s) {
            try {
                Integer.parseInt(s.trim());
                return true;
            } catch (NumberFormatException ex) {
                return false;
            }
        }
    }

    public class Computer {

        private String computerChoice = "";

        public Integer GetRandomBit() {
            int ourByteAmount = GetByteAmountOfMoves();
            int randomBit;
            int randomByte = secureRandom.nextInt(ourByteAmount + 1);
            do {
                randomBit = secureRandom.nextInt(randomByte * 8 + 1);
            } while (randomBit == 0 || randomBit > possibleMovesSize);
            computerChoice = possibleMoves.get(randomBit);
            return randomBit;
        }

        public Integer GetByteAmountOfMoves() {
            int byteAmount = 1;
            while (possibleMovesSize > byteAmount * 8) {
                byteAmount++;
            }
            return byteAmount;
        }

        public String GetComputerPath() {
            return computerChoice;
        }
    }

    public String GenerateSecretKey() {
        byte[] values = new byte[16];
        secureRandom.nextBytes(values);
        StringBuilder sb = new StringBuilder();
        for (byte b : values) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String CalculateHMAC(String data, String key) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), HMAC_SHA512);
            Mac mac = Mac.getInstance(HMAC_SHA512);
            mac.init(secretKeySpec);
            return ToHexString(mac.doFinal(data.getBytes("UTF-8")));
        } catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
            return "";
        }
    }

    private static String ToHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public int ComparePlayersMoves(Integer usersMove, Integer computersMove) {
        int resUser = -1;
        int t = computersMove - usersMove;
        if (t == 0) {
            resUser = 0;
        }
        if (t > 0) {
            resUser = t % 2 == 1 ? 2 : 1;
        }
        if (t < 0) {
            resUser = (t + possibleMovesSize) % 2 == 1 ? 2 : 1;
        }
        return resUser;
    }

    public void GetSelectedMoves() {
        String key = GenerateSecretKey();
        computerMove = computer.GetRandomBit();
        String hmac = CalculateHMAC(computer.GetComputerPath(), key);

        System.out.println("HMAC: " + hmac + "\n");

        System.out.println("Computer has done its move. Now it's your turn: \n");
        userMove = user.GetUserPath(possibleMoves);

        System.out.println("Computer path: " + computer.GetComputerPath()+ "\n");
        System.out.println("Key: " + key + "\n");

        int movesComparison = ComparePlayersMoves(userMove, computerMove);
        ResultsOfGame(movesComparison);
        
        PlayOrExit();
    }

    public void PlayOrExit() {
        System.out.println("Would you like play again or exit? \n");
        String userInput = scn.nextLine();
        boolean letsPlay = userInput.toUpperCase().charAt(0) == 'P';
        if (letsPlay) {
            System.out.println();
            GetSelectedMoves();
        } else {
            exitFromApplication();
        }
    }

    public void ResultsOfGame(int resultOfComparison) {
        String resultToShow = "";
        switch (resultOfComparison) {
            case 0:
                resultToShow = "Tie!";
                break;
            case 1:
                resultToShow = possibleMoves.get(computerMove) + " beats " + possibleMoves.get(userMove) + ". You lose.";
                break;
            case 2:
                resultToShow = possibleMoves.get(userMove) + " beats " + possibleMoves.get(computerMove) + ". You won!";
                break;
        }
        System.out.println("---> "+ resultToShow+ "\n");
        System.out.println("Don't believe it? Check it here: www.freeformatter.com/hmac-generator.html#ad-output\n");
    }

    public void exitFromApplication() {
        System.exit(0);
    }
}
