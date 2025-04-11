import com.mduffin95.Team
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.io.StringWriter

fun getHtml(teams: List<Team>) : String {
    val teamsSorted = teams.sortedBy { it.name } // Sort teams alphabetically
    val sw = StringWriter()
    sw.use { writer ->
        writer.appendHTML().html {
            head {
                meta(charset = "UTF-8")
                meta {
                    name = "viewport"
                    content = "width=device-width, initial-scale=1.0"
                }
                title("Team Calendar Lookup")
                style {
                    unsafe {
                        +"""
                        body {
                          font-family: Arial, sans-serif;
                          text-align: center;
                          background-color: #f4f4f9;
                          margin: 50px;
                        }
                        .container {
                          background: white;
                          padding: 20px;
                          border-radius: 10px;
                          box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
                          max-width: 400px;
                          margin: auto;
                          display: flex;
                          flex-direction: column;
                          align-items: center;
                        }
                        select, button {
                          margin: 10px;
                          padding: 10px;
                          font-size: 16px;
                          width: calc(100% - 20px);
                          border-radius: 5px;
                          border: 1px solid #ccc;
                          text-align: center;
                        }
                        button {
                          background-color: #28a745;
                          color: white;
                          border: none;
                          cursor: pointer;
                        }
                        button:hover {
                          background-color: #218838;
                        }
                        #result {
                          margin-top: 20px;
                          font-size: 20px;
                          font-weight: bold;
                          color: #555;
                          word-break: break-word;
                        }
                        #copyButton {
                          margin-top: 10px;
                          background-color: #007bff;
                        }
                        #copyButton:hover {
                          background-color: #0069d9;
                        }
                        #copyMessage {
                          font-size: 14px;
                          color: #28a745;
                          margin-top: 5px;
                        }
                        #instructions {
                          display: none;
                          margin-top: 20px;
                          text-align: left;
                          font-size: 14px;
                          color: #333;
                        }
                        #instructions ol {
                          padding-left: 20px;
                        }
                        """
                    }
                }
            }

            body {
                div("container") {
                    h1 { +"Team Calendar Lookup" }

                    label {
                        htmlFor = "teamSelect"
                        +"Choose a team:"
                    }

                    select {
                        id = "teamSelect"
                        option {
                            value = ""
                            +"-- Select a Team --"
                        }
                        teamsSorted.forEach { (id, name) ->
                            option {
                                value = id.toString()
                                +name
                            }
                        }
                    }

                    button {
                        onClick = "lookupTeam()"
                        +"Get Calendar!"
                    }

                    p {
                        id = "result"
                    }

                    button {
                        id = "copyButton"
                        style = "display: none;"
                        onClick = "copyToClipboard()"
                        +"Copy to Clipboard"
                    }

                    p {
                        id = "copyMessage"
                    }

                    div {
                        id = "instructions"
                        unsafe {
                            +"""
                            <h3>How to Add This Calendar</h3>
                            <h4>To Google Calendar:</h4>
                            <ol>
                              <li>Copy the calendar URL above</li>
                              <li><a href="https://calendar.google.com/calendar/u/0/r/settings/addbyurl" target="_blank">Go to Add by URL</a></li>
                              <li>Paste the URL and click "Add calendar"</li>
                              <li>Open the Google Calendar app and go to "Settings"</li>
                              <li>Click on the newly added calendar (you may have to click "show more" to see it)</li>
                              <li>Toggle on "Sync"</li>
                            </ol>
                            <h4>To iPhone:</h4>
                            <ol>
                              <li>Tap the calendar link above</li>
                              <li>iOS will ask to subscribe — tap "Subscribe"</li>
                            </ol>
                            """
                        }
                    }

                    script {
                        unsafe {
                            +"""
                            let currentUrl = '';

                            function lookupTeam() {
                              const select = document.getElementById("teamSelect");
                              const result = document.getElementById("result");
                              const instructions = document.getElementById("instructions");
                              const copyButton = document.getElementById("copyButton");
                              const copyMessage = document.getElementById("copyMessage");
                              const teamID = select.value;

                              if (teamID) {
                                currentUrl = `https://spawtz-calendar-calendarbucket-xvug78kqlk9n.s3.eu-west-1.amazonaws.com/calendar/v1/${'$'}{teamID}.ics`;
                                result.innerHTML = `<a href="${'$'}{currentUrl}" target="_blank">${'$'}{currentUrl}</a>`;
                                instructions.style.display = "block";
                                copyButton.style.display = "inline-block";
                                copyMessage.textContent = '';
                              } else {
                                result.textContent = "Please select a team.";
                                instructions.style.display = "none";
                                copyButton.style.display = "none";
                                copyMessage.textContent = '';
                              }
                            }

                            function copyToClipboard() {
                              if (!currentUrl) return;

                              navigator.clipboard.writeText(currentUrl).then(() => {
                                document.getElementById("copyMessage").textContent = "Copied to clipboard!";
                              }).catch(err => {
                                document.getElementById("copyMessage").textContent = "Failed to copy.";
                                console.error(err);
                              });
                            }
                            """
                        }
                    }
                }
            }
        }
    }

    return sw.toString()
}
