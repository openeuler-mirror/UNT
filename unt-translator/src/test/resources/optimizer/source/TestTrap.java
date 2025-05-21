public class TestTrap {
    public void checkException() {
        try {
            try {
                System.out.println("testException");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
