package in.nic.npi.exception;

public class PathNotFoundException extends Exception {
    String message;

    public PathNotFoundException() {
    }

    public PathNotFoundException(String msg) {
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
