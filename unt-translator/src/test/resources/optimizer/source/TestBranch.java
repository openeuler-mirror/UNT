public class TestBranch {
    public void testIf(int i) {
        if (i > 10) {
            System.out.println("big than 10");
        } else {
            System.out.println("less or equals than 10");
        }
    }

    public void testSwitch(int i) {
        switch (i) {
            case 0:
                System.out.println(0);
                break;
            case 1:
                System.out.println(1);
                break;
            case 2:
                System.out.println(2);
                break;
            case 3:
                System.out.println(3);
                break;
            default:
                System.out.println("default");
        }
    }

    public void testLoop() {
        for (int i = 0; i < 10; i++) {
            System.out.println(i);
        }
    }
}
