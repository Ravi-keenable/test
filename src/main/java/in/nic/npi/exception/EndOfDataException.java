package in.nic.npi.exception;

public class EndOfDataException extends Exception {
    String message;

    public EndOfDataException() {
    }

    public EndOfDataException(String msg) {
        this.message = msg;
    }

    @Override
    public String getMessage() {
        if (message == null)
            return super.getMessage();
        else
            return message;
    }

    @Override
    public void printStackTrace() {
        super.printStackTrace();
    }

}
