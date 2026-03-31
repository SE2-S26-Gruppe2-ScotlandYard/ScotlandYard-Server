 # Scotland Yard Server

Server in Java für das Spiel Scotland Yard – [621.252] SE2 Gruppe 2.
Basierend auf dem bereitgestellten [Demo Projekt](https://github.com/AAU-SE2/WebSocketDemo-Server).

## CI/CD

GitHub Actions führt bei jedem Push auf `main` automatisch aus:
1. Build
2. Unit Tests + Coverage Report
3. SonarCube und SonarCloud Scan

## Branch-Workflow

- Branches: `<branchTyp>/<beschreibung>`
- Commit-Convention: `<[#IssueNummer falls vorhanden]> <typ> <beschreibung>`
- Merges nur via Pull Request (kein Squash/Rebase)
- `main` ist protected und muss jederzeit lauffähig sein

## Zusätzliche Resourcen
- [GitHub Markdown Guide](https://guides.github.com/features/mastering-markdown/) 
- [Git Cheat Sheet](https://education.github.com/git-cheat-sheet-education.pdf)
- [How to Use Git Branches](https://git-scm.com/book/en/v2/Git-Branching-Basic-Branching-and-Merging)
