import { useEffect, useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Autocomplete,
  Box,
  Typography,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Chip,
  IconButton,
  Tooltip,
  CircularProgress,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import SendIcon from '@mui/icons-material/Send';
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh';
import { api, type SendMessageRequest, type SendMessageResponse } from 'shared';

export const FIPA_PERFORMATIVES = [
  'INFORM',
  'REQUEST',
  'QUERY-IF',
  'QUERY-REF',
  'PROPOSE',
  'CFP',
  'AGREE',
  'REFUSE',
  'FAILURE',
  'NOT-UNDERSTOOD',
  'CONFIRM',
  'DISCONFIRM',
  'ACCEPT-PROPOSAL',
  'REJECT-PROPOSAL',
  'SUBSCRIBE',
  'CANCEL',
  'PROXY',
  'PROPAGATE',
  'INFORM-IF',
  'INFORM-REF',
  'REQUEST-WHEN',
  'REQUEST-WHENEVER',
];

export const COMMON_PROTOCOLS = [
  'fipa-request',
  'fipa-query',
  'fipa-propose',
  'fipa-subscribe',
  'fipa-contract-net',
  'fipa-iterated-contract-net',
  'none',
];

export const COMMON_LANGUAGES = [
  'fipa-sl',
  'json',
  'plain-text',
  'xml',
];

interface TemplateOption {
  label: string;
  performative: string;
  protocol?: string;
  content: string;
}

const TEMPLATES: Record<string, TemplateOption> = {
  custom: { label: 'Custom / Blank', performative: 'INFORM', content: '' },
  request: {
    label: 'FIPA-Request (Execute action)',
    performative: 'REQUEST',
    protocol: 'fipa-request',
    content: '((action (agent-identifier :name receiver) (do-task :param "value")))',
  },
  query: {
    label: 'FIPA-Query (Query knowledge/state)',
    performative: 'QUERY-IF',
    protocol: 'fipa-query',
    content: '((current-status))',
  },
  propose: {
    label: 'FIPA-Propose (Make proposal)',
    performative: 'PROPOSE',
    protocol: 'fipa-propose',
    content: '((proposal :price 100 :service "standard"))',
  },
  inform: {
    label: 'FIPA-Inform (Share state/fact)',
    performative: 'INFORM',
    content: '((status-update :state "READY" :timestamp 0))',
  },
  agree: {
    label: 'FIPA-Agree (Agree to perform request)',
    performative: 'AGREE',
    protocol: 'fipa-request',
    content: '((agree (action (agent-identifier :name receiver))))',
  },
  refuse: {
    label: 'FIPA-Refuse (Refuse request)',
    performative: 'REFUSE',
    protocol: 'fipa-request',
    content: '((refuse :reason "unauthorized-or-busy"))',
  },
};

interface MessageComposerDialogProps {
  open: boolean;
  onClose: () => void;
  initialReceiver?: string;
  initialPerformative?: string;
  initialConversationId?: string;
  initialInReplyTo?: string;
  initialProtocol?: string;
  initialOntology?: string;
  initialContent?: string;
  onSent?: (res: SendMessageResponse) => void;
}

export default function MessageComposerDialog({
  open,
  onClose,
  initialReceiver,
  initialPerformative = 'INFORM',
  initialConversationId = '',
  initialInReplyTo = '',
  initialProtocol = '',
  initialOntology = '',
  initialContent = '',
  onSent,
}: MessageComposerDialogProps) {
  const [performative, setPerformative] = useState(initialPerformative);
  const [receivers, setReceivers] = useState<string[]>(initialReceiver ? [initialReceiver] : []);
  const [sender, setSender] = useState('');
  const [content, setContent] = useState(initialContent);
  const [protocol, setProtocol] = useState(initialProtocol);
  const [language, setLanguage] = useState('fipa-sl');
  const [ontology, setOntology] = useState(initialOntology);
  const [conversationId, setConversationId] = useState(initialConversationId);
  const [inReplyTo, setInReplyTo] = useState(initialInReplyTo);
  const [replyWith, setReplyWith] = useState('');
  const [availableAgents, setAvailableAgents] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Sync initial values when opened
  useEffect(() => {
    if (open) {
      setPerformative(initialPerformative);
      setReceivers(initialReceiver ? [initialReceiver] : []);
      setConversationId(initialConversationId);
      setInReplyTo(initialInReplyTo);
      setProtocol(initialProtocol);
      setOntology(initialOntology);
      setContent(initialContent);
      setError(null);

      // Fetch active agents for autocomplete
      api.agents.list({ detail: false })
        .then((res) => {
          const names = res.agents.map((a) => a.name);
          setAvailableAgents(names);
        })
        .catch(() => {});
    }
  }, [open, initialReceiver, initialPerformative, initialConversationId, initialInReplyTo, initialProtocol, initialOntology, initialContent]);

  const handleTemplateChange = (templateKey: string) => {
    const tmpl = TEMPLATES[templateKey];
    if (!tmpl) return;
    setPerformative(tmpl.performative);
    if (tmpl.protocol) setProtocol(tmpl.protocol);
    if (tmpl.content) setContent(tmpl.content);
    if (!conversationId && templateKey !== 'custom') {
      generateConversationId();
    }
  };

  const generateConversationId = () => {
    const rand = Math.random().toString(36).substring(2, 8);
    setConversationId(`conv-${Date.now().toString(36)}-${rand}`);
  };

  const handleSend = async () => {
    if (receivers.length === 0) {
      setError('At least one recipient is required');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const payload: SendMessageRequest = {
        performative,
        sender: sender.trim() || undefined,
        receivers,
        content,
        language: language || undefined,
        ontology: ontology.trim() || undefined,
        protocol: protocol && protocol !== 'none' ? protocol.trim() : undefined,
        conversationId: conversationId.trim() || undefined,
        inReplyTo: inReplyTo.trim() || undefined,
        replyWith: replyWith.trim() || undefined,
      };

      const res = await api.messages.send(payload);
      onSent?.(res);
      onClose();
    } catch (e: any) {
      setError(e.message || 'Failed to send ACL message');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <SendIcon color="primary" />
          <Typography variant="h6">Compose ACL Message</Typography>
        </Box>
        <Chip label="FIPA-2000 Compliant" size="small" color="primary" variant="outlined" />
      </DialogTitle>

      <DialogContent dividers>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {error && (
            <Typography color="error" variant="body2" sx={{ p: 1, bgcolor: 'error.dark', borderRadius: 1 }}>
              {error}
            </Typography>
          )}

          {/* Template & Quick Setup */}
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
            <FormControl fullWidth size="small">
              <InputLabel>Message Template</InputLabel>
              <Select
                defaultValue="custom"
                label="Message Template"
                onChange={(e) => handleTemplateChange(e.target.value)}
              >
                {Object.entries(TEMPLATES).map(([key, tmpl]) => (
                  <MenuItem key={key} value={key}>
                    {tmpl.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <FormControl sx={{ minWidth: 200 }} size="small">
              <InputLabel>Performative</InputLabel>
              <Select
                value={performative}
                label="Performative"
                onChange={(e) => setPerformative(e.target.value)}
              >
                {FIPA_PERFORMATIVES.map((p) => (
                  <MenuItem key={p} value={p}>
                    {p}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>

          {/* Receivers & Sender */}
          <Autocomplete
            multiple
            freeSolo
            options={availableAgents}
            value={receivers}
            onChange={(_, newValue) => setReceivers(newValue)}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Recipients (AID / Agent Names)"
                placeholder="Type or select agent"
                size="small"
                helperText="Select active platform agents or type foreign agent AIDs"
              />
            )}
          />

          <Autocomplete
            freeSolo
            options={availableAgents}
            value={sender}
            onChange={(_, newValue) => setSender(newValue || '')}
            onInputChange={(_, newInputValue) => setSender(newInputValue)}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Sender AID (Optional)"
                placeholder="Defaults to ui-user@<platformID>"
                size="small"
              />
            )}
          />

          {/* Message Content */}
          <TextField
            label="Message Content"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            multiline
            rows={4}
            fullWidth
            placeholder="ACL message body payload (SL expression, JSON, or text)"
          />

          {/* Collapsible Protocol Details */}
          <Accordion variant="outlined">
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Typography variant="subtitle2" color="text.secondary">
                Protocol &amp; Envelope Details (Conversation ID, Language, Ontology)
              </Typography>
            </AccordionSummary>
            <AccordionDetails>
              <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
                <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                  <TextField
                    label="Conversation ID"
                    size="small"
                    fullWidth
                    value={conversationId}
                    onChange={(e) => setConversationId(e.target.value)}
                  />
                  <Tooltip title="Generate Unique ID">
                    <IconButton onClick={generateConversationId} color="primary" size="small">
                      <AutoFixHighIcon />
                    </IconButton>
                  </Tooltip>
                </Box>

                <FormControl fullWidth size="small">
                  <InputLabel>Protocol</InputLabel>
                  <Select
                    value={protocol || 'none'}
                    label="Protocol"
                    onChange={(e) => setProtocol(e.target.value)}
                  >
                    {COMMON_PROTOCOLS.map((proto) => (
                      <MenuItem key={proto} value={proto}>
                        {proto}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>

                <FormControl fullWidth size="small">
                  <InputLabel>Language</InputLabel>
                  <Select
                    value={language}
                    label="Language"
                    onChange={(e) => setLanguage(e.target.value)}
                  >
                    {COMMON_LANGUAGES.map((lang) => (
                      <MenuItem key={lang} value={lang}>
                        {lang}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>

                <TextField
                  label="Ontology"
                  size="small"
                  fullWidth
                  value={ontology}
                  onChange={(e) => setOntology(e.target.value)}
                  placeholder="e.g. FIPA-Agent-Management"
                />

                <TextField
                  label="In-Reply-To"
                  size="small"
                  fullWidth
                  value={inReplyTo}
                  onChange={(e) => setInReplyTo(e.target.value)}
                  placeholder="Referenced conversation or message ID"
                />

                <TextField
                  label="Reply-With"
                  size="small"
                  fullWidth
                  value={replyWith}
                  onChange={(e) => setReplyWith(e.target.value)}
                  placeholder="Token to identify reply"
                />
              </Box>
            </AccordionDetails>
          </Accordion>
        </Box>
      </DialogContent>

      <DialogActions sx={{ p: 2 }}>
        <Button onClick={onClose} color="inherit">
          Cancel
        </Button>
        <Button
          onClick={handleSend}
          variant="contained"
          color="primary"
          startIcon={loading ? <CircularProgress size={18} color="inherit" /> : <SendIcon />}
          disabled={loading || receivers.length === 0}
        >
          {loading ? 'Sending...' : 'Send Message'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
