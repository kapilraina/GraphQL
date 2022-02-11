
var clientWebSocketurl = "";
try {
    if (window.location.protocol === "http:") {
        clientWebSocketurl = "ws://" + window.location.host + "/graphql";
    }
    if (window.location.protocol === "https:") {
        clientWebSocketurl = "wss://" + window.location.host + "/graphql";
    }

} catch (error) {
    console.error("Error Opening Socket : " + error);
}


class Graphql {

    constructor(url) {
        this.client = graphqlWs.createClient({ url: url })
    }


    subscribe(q, callback) {


        this.client.subscribe(
            { query: q },
            {
                next: callback,
                error: (err) => console.error("error " + JSON.stringify(err)),
                complete: () => console.log("subscription is complete")
            }
        )
    }

    async query(queryString) {
        return await new Promise((resolve, reject) => {
            let result;
            this.client.subscribe(
                {
                    query: queryString
                },
                {
                    next: (data) => (result = data),
                    error: reject,
                    complete: () => resolve(result)
                }
            );
        }

        )
    }

}

let s_gql = new Graphql(clientWebSocketurl);
const qs = "subscription{\n" +
    "subjectEventSubscription(subjectid:1)\n" +
    "{\n" +
    "subject{\n" +
    "name\n" +
    "}\n" +
    "type\n" +
    "channel\n" +
    "starttime\n" +
    "}\n" +
    "}";
s_gql.subscribe(qs, (result) => {
    console.log(JSON.stringify(result));
    logmessage(result);
});

const q = "subscription{\n" +
    "globalEventSubscription\n" +
    "{\n" +
    "subject{\n" +
    "name\n" +
    "}\n" +
    "type\n" +
    "channel\n" +
    "starttime\n" +
    "}\n" +
    "}";

let g_gql = new Graphql(clientWebSocketurl);
g_gql.subscribe(q, (result) => {
    console.log(JSON.stringify(result));
    logevent(result);
});


function logmessage(message) {

    $('#s_sub').append(
        "<div class='column middle'><label class='message'>" + JSON.stringify(message['data']['subjectEventSubscription']) + "</label></div>"
    );
}


function logevent(message) {

    $('#g_sub').append(
        "<div class='column middle'><label class='message'>" + JSON.stringify(message['data']['globalEventSubscription']) + "</label></div>"
    );
}
