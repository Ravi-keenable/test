package in.nic.npi.exception;

public class InvalidOffsetException extends Exception {
    String message;

    public InvalidOffsetException() {
    }

    public InvalidOffsetException(String msg) {
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
