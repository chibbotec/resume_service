package bitcamp.project1.command;

import bitcamp.project1.Prompt.Prompt;
import bitcamp.project1.util.ArrayList;
import bitcamp.project1.vo.Finance;
import bitcamp.project1.vo.Wallet;

import java.time.LocalDate;

public class OutcomeCommand {
    private final int PROCESS_LIST = 0;
    private final int PROCESS_SEARCH = 1;
    private final int PROCESS_UPDATE = 2;
    private final int PROCESS_DELETE = 3;

    ArrayList outcomeList = new ArrayList();
    Object[] wallet;

    public OutcomeCommand(Object[] list) {
        this.wallet = list;
    }

    public void autoOutcomeData() {
        Finance outcome = new Finance();
        outcome.setKindOfCome("출금");
        outcome.setDate(LocalDate.parse("2024-06-27"));
        outcome.setCategory("식비");
        outcome.setAccount(0);
        outcome.setAmount(300);
        outcome.setNo(Finance.getSeqNo());
        outcomeList.add(outcome);

        Finance outcome1 = new Finance();
        outcome1.setKindOfCome("출금");
        outcome1.setDate(LocalDate.parse("2024-06-28"));
        outcome1.setCategory("서적");
        outcome1.setAccount(1);
        outcome1.setAmount(700);
        outcome1.setNo(Finance.getSeqNo());
        outcomeList.add(outcome1);

        Finance outcome2 = new Finance();
        outcome2.setKindOfCome("출금");
        outcome2.setDate(LocalDate.parse("2024-06-28"));
        outcome2.setCategory("아이패드");
        outcome2.setAccount(2);
        outcome2.setAmount(5000);
        outcome2.setNo(Finance.getSeqNo());
        outcomeList.add(outcome2);
    }

    public void executeOutcomeCommand(String command) {
        System.out.printf("[%s]\n", command);
        switch (command) {
            case "등록":
                createIncome();
                break;
            case "목록":
                listOutcome();
                break;
            case "조회":
                searchOutcome();
                break;
            case "변경":
                updateOutcome();
                break;
            case "삭제":
                deleteOutcome();
                break;
        }
    }

    private void createIncome() {
        Finance outcome = new Finance();
        outcome.setKindOfCome("지출");
        outcome.setDate(Prompt.inputDate("지출일(yyyy-MM-dd)?"));
        outcome.setAmount(Prompt.inputInt("지출금액?"));
        setWallet(outcome);
        outcome.setCategory(Prompt.input("카테고리?"));
        outcome.setNo(Finance.getSeqNo());
        outcomeList.add(outcome);
    }

    private void listOutcome() {
        System.out.println("날짜 항목 금액");
        printNoList(PROCESS_LIST);
    }

    private void searchOutcome() {
        printNoList(PROCESS_SEARCH);
        int outcomeNo = Prompt.inputInt("조회 할 지출 번호?");
        Finance searchedOutcome =
                (Finance) outcomeList.get((outcomeList.indexOf(new Finance(outcomeNo))));
        if (searchedOutcome == null) {
            System.out.println("없는 지출 번호입니다.");
            return;
        }
        System.out.printf("지출날짜 : %s\n", searchedOutcome.getDate());
        System.out.printf("지출금액 : %s\n", searchedOutcome.getAmount());
        Wallet account = (Wallet) wallet[searchedOutcome.getAccount()];
        System.out.printf("결제방법 : %s\n", account.getAssetType());
        System.out.printf("카테고리 : %s\n", searchedOutcome.getCategory());
    }

    private void updateOutcome() {
        printNoList(PROCESS_UPDATE);
        int outcomeNo = Prompt.inputInt("변경 할 지출 번호?");
        Finance updateOutcome =
                (Finance) outcomeList.get((outcomeList.indexOf(new Finance(outcomeNo))));
        if (updateOutcome == null) {
            System.out.println("없는 지출 번호입니다.");
            return;
        }
        updateOutcome.setDate(Prompt.inputDate("지출일(%s):", updateOutcome.getDate()));
        updateOutcome.setAmount(Prompt.inputInt("지출금액(%s):", updateOutcome.getAmount()));
        setWallet(updateOutcome);
        updateOutcome.setCategory(Prompt.input("카테고리(%s):", updateOutcome.getCategory()));
        System.out.println("변경 완료했습니다.");
    }

    private void deleteOutcome() {
        printNoList(PROCESS_DELETE);
        int outcomeNo = Prompt.inputInt("삭제 할 지출 번호?");
        Finance deletedOutcome =
                (Finance) outcomeList.get((outcomeList.indexOf(new Finance(outcomeNo))));
        if (deletedOutcome != null) {
            outcomeList.remove(outcomeList.indexOf(deletedOutcome));
            System.out.println("삭제 완료했습니다.");
        } else {
            System.out.println("없는 지출 번호입니다.");
        }
    }

    private void printNoList(int processNo) {
        for (Object object : outcomeList.toArray()) {
            Finance income = (Finance) object;
            switch (processNo) {
                case PROCESS_UPDATE:
                case PROCESS_DELETE:
                case PROCESS_SEARCH:
                    System.out.printf("%d. ", income.getNo());
                case PROCESS_LIST:
                    System.out.printf("%s %s %s\n", income.getDate(), income.getCategory(),
                            income.getAmount());
                    break;
                default:
            }
        }
    }

    private void setWallet(Finance outcome) {
        for (int i = 0; i < wallet.length; i++) {
            Wallet value = (Wallet) wallet[i];
            if (value == null) {
                continue;
            }
            System.out.printf("%d. %s\n", i + 1, value.getAssetType());
        }

        while (true) {
            int no = Prompt.inputInt("결제방법?");
            if ((no < 0 || no >= wallet.length) || wallet[no - 1] == null) {
                System.out.println("유효한 결제방법이 아닙니다.");
            } else {
                outcome.setAccount(no - 1);
                break;
            }
        }
    }

    public ArrayList getOutcomeList() {
        return outcomeList;
    }
}
