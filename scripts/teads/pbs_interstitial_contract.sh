#! /bin/bash

# Contract check for the Teads/Outbrain test Prebid Server (interstitial).
# Exit codes: 0 = all MUST passed, 1 = a MUST failed, 2 = server unreachable or PBS_HOST unset (skip).
#
# Env:
#   PBS_HOST       auction URL (required)
#   EXPECTED_SEAT  expected seat / hb_bidder (default: outbrain)
#   OUT_DIR        where request/response JSON are saved

set -u

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -z "${PBS_HOST:-}" ]; then
    echo "⏭️  PBS_HOST not set"
    exit 2
fi
EXPECTED_SEAT="${EXPECTED_SEAT:-outbrain}"
OUT_DIR="${OUT_DIR:-$SCRIPT_DIR/out}"
REQUEST="$SCRIPT_DIR/fixtures/interstitial_request.json"
RESPONSE="$OUT_DIR/interstitial_response.json"

MUST_FAILED=0

function pass { echo "✅ $1"; }
function must_fail { echo "❌ MUST   $1"; MUST_FAILED=1; }
function should_fail { echo "⚠️  SHOULD $1"; }

function check_must {
    if jq -e "$2" "$RESPONSE" > /dev/null; then pass "$1"; else must_fail "$1"; fi
}

function check_should {
    if jq -e "$2" "$RESPONSE" > /dev/null; then pass "$1"; else should_fail "$1"; fi
}

mkdir -p "$OUT_DIR"
cp "$REQUEST" "$OUT_DIR/interstitial_request.json"

STATUS_URL="${PBS_HOST%/openrtb2/auction}/status"
if ! curl -s -f -m 5 -o /dev/null "$STATUS_URL"; then
    echo "⏭️  PBS unreachable ($STATUS_URL) - VPN?"
    exit 2
fi

read -r HTTP_CODE TIME_TOTAL < <(curl -s -m 10 -o "$RESPONSE" -w "%{http_code} %{time_total}" \
    -H "Content-Type: application/json" -d @"$REQUEST" "$PBS_HOST")

echo "PBS:      $PBS_HOST"
echo "Response: $RESPONSE"
echo

if [ "$HTTP_CODE" = "200" ] && awk "BEGIN { exit !($TIME_TOTAL < 2) }"; then
    pass "A1 HTTP 200 in ${TIME_TOTAL}s"
else
    must_fail "A1 HTTP $HTTP_CODE in ${TIME_TOTAL}s (want 200 in < 2s)"
    exit 1
fi

BID='.seatbid[0].bid[0]'
check_must "A2 bid exists for imp-1" "$BID.impid == \"imp-1\""
check_must "A3 price > 0" "$BID.price > 0"
check_must "A4 adm is HTML, w/h set, type banner" \
    "($BID.adm | contains(\"<\")) and $BID.w > 0 and $BID.h > 0 and $BID.ext.prebid.type == \"banner\""
check_must "A5 targeting has hb_pb, hb_bidder, hb_size" \
    "$BID.ext.prebid.targeting | has(\"hb_pb\") and has(\"hb_bidder\") and has(\"hb_size\")"
check_should "A6 seat and hb_bidder == $EXPECTED_SEAT" \
    ".seatbid[0].seat == \"$EXPECTED_SEAT\" and $BID.ext.prebid.targeting.hb_bidder == \"$EXPECTED_SEAT\""
check_should "A7 no bidder errors, bidder in responsetimemillis" \
    "(.ext.errors // {} | has(\"$EXPECTED_SEAT\") | not) and (.ext.responsetimemillis | has(\"$EXPECTED_SEAT\"))"
check_should "A8 win/imp event URLs present" \
    "($BID.ext.prebid.events.win | length > 0) and ($BID.ext.prebid.events.imp | length > 0)"

# Not a criterion: Rendering API reads adm directly, but GAM/Original API flows need hb_cache_id.
jq -r '.ext.errors.prebid[]?.message | "ℹ️  server: " + .' "$RESPONSE"

exit $MUST_FAILED
